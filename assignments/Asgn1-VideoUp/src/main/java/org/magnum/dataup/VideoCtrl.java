/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoCtrl {

	private static final AtomicLong currentId = new AtomicLong(0L);
	private Map<Long, Video> videos = new HashMap<Long, Video>();

	@ResponseStatus(value = org.springframework.http.HttpStatus.NOT_FOUND)
	private final class ResourceNotFoundException extends RuntimeException {
		/**
		 * This throws a 404 error
		 */
		private static final long serialVersionUID = -883750702876139000L;
		// class definition
	}

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video entity) {
		return save(entity);
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable("id") long id,
			@RequestParam("data") MultipartFile videoData, HttpServletResponse response)
			throws IOException {
		Video video = videos.get(id);

		if (video == null) {
			throw new ResourceNotFoundException();
		}
		
		try {
			saveSomeVideo(video, videoData);
			response.setStatus(HttpStatus.OK.value());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new VideoStatus(VideoState.READY);
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public void getVideoData(@PathVariable("id") long id,
			HttpServletResponse response) {
		Video video = videos.get(id);

		if (video == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
		} else {
			try {
				serveSomeVideo(video, response);
			} catch (IOException e) {
				e.printStackTrace();
				response.setStatus(404);
			}
		}

	}

	public void serveSomeVideo(Video v, HttpServletResponse response)
			throws IOException {

		// Of course, you would need to send some headers, etc. to the

		// client too!

		// ...

		response.setStatus(200);
		response.setContentType(v.getContentType());

		VideoFileManager.get().copyVideoData(v, response.getOutputStream());

	}

	public void saveSomeVideo(Video video, MultipartFile videoData)
			throws IOException {
		VideoFileManager.get().saveVideoData(video, videoData.getInputStream());
	}

	public Video save(Video entity) {
		checkAndSetId(entity);
		entity.setDataUrl(getDataUrl(entity.getId()));
		videos.put(entity.getId(), entity);
		return entity;
	}

	private void checkAndSetId(Video entity) {
		if (entity.getId() == 0) {
			entity.setId(currentId.incrementAndGet());
		}
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
