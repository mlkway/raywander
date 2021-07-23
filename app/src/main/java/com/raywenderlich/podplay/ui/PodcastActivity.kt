/*
 * Copyright (c) 2021 Razeware LLC
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *   Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 *   distribute, sublicense, create a derivative work, and/or sell copies of the
 *   Software in any work that is designed, intended, or marketed for pedagogical or
 *   instructional purposes related to programming, coding, application development,
 *   or information technology.  Permission for such use, copying, modification,
 *   merger, publication, distribution, sublicensing, creation of derivative works,
 *   or sale is expressly withheld.
 *
 *   This project and source code may use libraries or frameworks that are
 *   released under various Open-Source licenses. Use of those libraries and
 *   frameworks are governed by their own individual licenses.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */

package com.raywenderlich.podplay.ui


import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.work.*
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.databinding.FragmentEpisodePlayerBinding
import com.raywenderlich.podplay.model.RandomPod
import com.raywenderlich.podplay.service.PodplayMediaCallback
import com.raywenderlich.podplay.service.PodplayMediaService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class PodcastActivity : AppCompatActivity() {


  private val podcastViewModel: PodcastViewModel by viewModels()
  private lateinit var databinding: FragmentEpisodePlayerBinding
  private lateinit var mediaBrowser: MediaBrowserCompat
  private var mediaControllerCallback: MediaControllerCallback? = null
  private var progressAnimator: ValueAnimator? = null
  private var episodeDuration: Long = 0
  private var playerSpeed: Float = 1.0f
  private var draggingScrubber: Boolean = false
  private var mediaPlayer: MediaPlayer? = null
  private var playOnPrepare: Boolean = false


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    databinding = FragmentEpisodePlayerBinding.inflate(layoutInflater)
    setContentView(databinding.root)

    podcastViewModel.load()
    podcastViewModel.episode.observe(this){

    }
    initMediaBrowser()
    setupControls()
    updateControls()
    if (mediaBrowser.isConnected) {

      if (MediaControllerCompat.getMediaController(this) == null) {
        registerMediaController(mediaBrowser.sessionToken)
      }
      updateControlsFromController()
    } else {
      mediaBrowser.connect()
    }


  }


  override fun onStop() {
    super.onStop()
    progressAnimator?.cancel()
    if (MediaControllerCompat.getMediaController(this) != null) {
      mediaControllerCallback?.let {
        MediaControllerCompat.getMediaController(this)
          .unregisterCallback(it)
      }
    }

  }


  private fun updateControls() {
    // 1
    databinding.episodeTitleTextView.text = podcastViewModel.episode.value?.title
    // 2


    databinding.episodeDescTextView.text = """"""
    databinding.episodeDescTextView.movementMethod = ScrollingMovementMethod()
    // 3

    Glide.with(this)
      .load(podcastViewModel.episode.value?.image)
      .into(databinding.episodeImageView)


    mediaPlayer?.let {
      updateControlsFromController()
    }
  }


  private fun seekBy(seconds: Int) {

    val controller = MediaControllerCompat.getMediaController(this)
    val newPosition = controller.playbackState.position + seconds * 1000
    controller.transportControls.seekTo(newPosition)
  }


  private fun togglePlayPause() {
    playOnPrepare = true

    val controller = MediaControllerCompat.getMediaController(this)
    if (controller.playbackState != null) {
      if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
        controller.transportControls.pause()
      } else {
        podcastViewModel.episode.value?.let { startPlaying(it) }
      }
    } else {
      podcastViewModel.episode.value?.let { startPlaying(it) }
    }
  }




  private fun initMediaBrowser() {

    mediaBrowser = MediaBrowserCompat(this,
      ComponentName(this, PodplayMediaService::class.java),
      MediaBrowserCallBacks(),
      null)
  }


  private fun setupControls() {
    databinding.playToggleButton.setOnClickListener {
      togglePlayPause()
    }

    databinding.forwardButton.setOnClickListener {
      seekBy(30)
    }
    databinding.replayButton.setOnClickListener {
      seekBy(-10)
    }
    // 1
    databinding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        // 2
        databinding.currentTimeTextView.text = DateUtils.formatElapsedTime((progress / 1000).toLong())
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {
        // 3
        draggingScrubber = true
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        // 4
        draggingScrubber = false
        // 5

        val controller = MediaControllerCompat.getMediaController(this@PodcastActivity)
        if (controller.playbackState != null) {
          // 6
          controller.transportControls.seekTo(seekBar.progress.toLong())
        } else {
          // 7
          seekBar.progress = 0
        }
      }
    })
  }


  inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {

    override fun onConnected() {
      super.onConnected()
      registerMediaController(mediaBrowser.sessionToken)
      updateControlsFromController()
    }

    override fun onConnectionSuspended() {
      super.onConnectionSuspended()
      println("onConnectionSuspended")
    }

    override fun onConnectionFailed() {
      super.onConnectionFailed()
      println("onConnectionFailed")
    }
  }




  private fun registerMediaController(token: MediaSessionCompat.Token) {
    val mediaController = MediaControllerCompat(this, token)
    MediaControllerCompat.setMediaController(this, mediaController)
    mediaControllerCallback = MediaControllerCallback()
    mediaController.registerCallback(mediaControllerCallback!!)
  }


  inner class MediaControllerCallback : MediaControllerCompat.Callback() {



    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
      super.onMetadataChanged(metadata)
      metadata?.let { updateControlsFromMetadata(it) }
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
      val currentState = state ?: return
      handleStateChange(currentState.state, currentState.position, 1.0f)
    }
  }



  private fun updateControlsFromController() {

    val controller = MediaControllerCompat.getMediaController(this)
    if (controller != null) {
      val metadata = controller.metadata
      if (metadata != null) {
        handleStateChange(controller.playbackState.state,
          controller.playbackState.position, playerSpeed)
        updateControlsFromMetadata(controller.metadata)
      }
    }
  }

  private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
    episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
    databinding.endTimeTextView.text = "111"
    databinding.seekBar.max = episodeDuration.toInt()
  }

  private fun handleStateChange(state: Int, position: Long, speed: Float) {
    progressAnimator?.let {
      it.cancel()
      progressAnimator = null
    }
    val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
    databinding.playToggleButton.isActivated = isPlaying

    val progress = position.toInt()
    databinding.seekBar.progress = progress



    animateScrubber(progress, speed)
  }

  private fun animateScrubber(progress: Int, speed: Float) {

    val timeRemaining = ((episodeDuration - progress) / speed).toInt()
    if (timeRemaining < 0) {
      return
    }

    progressAnimator = ValueAnimator.ofInt(
      progress, episodeDuration.toInt())
    progressAnimator?.let { animator ->
      animator.duration = timeRemaining.toLong()
      animator.interpolator = LinearInterpolator()
      animator.addUpdateListener {
        if (draggingScrubber) {
          animator.cancel()
        } else {
          databinding.seekBar.progress = animator.animatedValue as Int
        }
      }
      animator.start()
    }
  }

  private fun startPlaying(episodeViewData: RandomPod) {

    val controller = MediaControllerCompat.getMediaController(this)
    val bundle = Bundle()
    bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
    bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, episodeViewData.id)
    bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, episodeViewData.image)

    controller.transportControls.playFromUri(Uri.parse(episodeViewData.audio), bundle)
  }



}



