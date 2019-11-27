/*
 * Copyright 2019 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.datatransfer.google.videos;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import org.datatransferproject.datatransfer.google.mediaModels.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.mediaModels.GoogleMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class GoogleVideosImporterTest {

  private static final String VIDEO_TITLE = "Model video title";
  private static final String VIDEO_DESCRIPTION = "Model video description";
  private static final String VIDEO_URI = "https://www.example.com/video.mp4";
  private static final String MP4_MEDIA_TYPE = "video/mp4";
  private static final String UPLOAD_TOKEN = "uploadToken";
  private static final String VIDEO_ID = "myId";
  private static final String RESULT_ID = "RESULT_ID";

  private GoogleVideosImporter googleVideosImporter;
  private GoogleVideosInterface googleVideosInterface;
  private ImageStreamProvider videoStreamProvider;
  private InputStream inputStream;

  @Before
  public void setUp() throws Exception {
    googleVideosInterface = mock(GoogleVideosInterface.class);

    when(googleVideosInterface.uploadVideoContent(
            Matchers.any(InputStream.class), Matchers.anyString()))
        .thenReturn(UPLOAD_TOKEN);

    final NewMediaItemResult mediaItemResult = mock(NewMediaItemResult.class);
    final GoogleMediaItem mediaItem = new GoogleMediaItem();
    mediaItem.setId(RESULT_ID);
    when(mediaItemResult.getMediaItem()).thenReturn(mediaItem);
    BatchMediaItemResponse batchMediaItemResponse =
        new BatchMediaItemResponse(new NewMediaItemResult[] {mediaItemResult});
    when(googleVideosInterface.createVideo(Matchers.any())).thenReturn(batchMediaItemResponse);

    inputStream = mock(InputStream.class);

    videoStreamProvider = mock(ImageStreamProvider.class);
    when(videoStreamProvider.get(Matchers.anyString())).thenReturn(inputStream);

    googleVideosImporter =
        new GoogleVideosImporter(null, googleVideosInterface, videoStreamProvider, null, null);
  }

  @Test
  public void exportVideo() throws Exception {
    // Set up
    VideoObject videoModel =
        new VideoObject(
            VIDEO_TITLE, VIDEO_URI, VIDEO_DESCRIPTION, MP4_MEDIA_TYPE, VIDEO_ID, null, false);

    // Run test
    final String resultId = googleVideosImporter.importSingleVideo(null, videoModel);

    // Check results
    verify(googleVideosInterface).uploadVideoContent(inputStream, "Copy of " + VIDEO_TITLE);
    verify(videoStreamProvider).get(VIDEO_URI);
    ArgumentCaptor<NewMediaItemUpload> uploadArgumentCaptor =
        ArgumentCaptor.forClass(NewMediaItemUpload.class);
    verify(googleVideosInterface).createVideo(uploadArgumentCaptor.capture());
    List<NewMediaItem> newMediaItems = uploadArgumentCaptor.getValue().getNewMediaItems();
    assertEquals(newMediaItems.size(), 1);
    NewMediaItem mediaItem = newMediaItems.get(0);
    assertEquals(mediaItem.getSimpleMediaItem().getUploadToken(), UPLOAD_TOKEN);
    assertEquals(RESULT_ID, resultId);
  }
}
