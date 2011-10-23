
package com.badlogic.gdx.backends.openal;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL11;

import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

import static org.lwjgl.openal.AL10.*;

/** @author Nathan Sweet */
public class OpenALAudioDevice implements AudioDevice {
	static private final int bufferSize = 512;
	static private final int bufferCount = 9;
	static private final int bytesPerSample = 2;
	static private final ByteBuffer tempBuffer = BufferUtils.createByteBuffer(bufferSize);

	private final OpenALAudio audio;
	private final int channels;
	private IntBuffer buffers;
	private int sourceID = -1;
	private int format, sampleRate;
	private boolean isPlaying;
	private float volume = 1;
	private float renderedSeconds, secondsPerBuffer;
	private byte[] bytes;

	public OpenALAudioDevice (OpenALAudio audio, int sampleRate, boolean isMono) {
		this.audio = audio;
		channels = isMono ? 1 : 2;
		this.format = channels > 1 ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16;
		this.sampleRate = sampleRate;
		secondsPerBuffer = (float)bufferSize / bytesPerSample / channels / sampleRate;
	}

	public void writeSamples (short[] samples, int offset, int numSamples) {
		if (bytes == null || bytes.length < numSamples * 2) bytes = new byte[numSamples * 2];
		for (int i = offset, ii = 0; i < numSamples; i++) {
			short sample = samples[i];
			bytes[ii++] = (byte)(sample & 0xFF);
			bytes[ii++] = (byte)((sample >> 8) & 0xFF);
		}
		writeSamples(bytes, 0, numSamples * 2);
	}

	public void writeSamples (float[] samples, int offset, int numSamples) {
		if (bytes == null || bytes.length < numSamples * 2) bytes = new byte[numSamples * 2];
		for (int i = offset, ii = 0; i < numSamples; i++) {
			float floatSample = samples[i];
			floatSample = MathUtils.clamp(floatSample, -1f, 1f);
			int intSample = (int)(floatSample * 32767);
			bytes[ii++] = (byte)(intSample & 0xFF);
			bytes[ii++] = (byte)((intSample >> 8) & 0xFF);
		}
		writeSamples(bytes, 0, numSamples * 2);
	}

	public void writeSamples (byte[] data, int offset, int length) {
		if (length < 0) throw new IllegalArgumentException("length cannot be < 0.");

		if (sourceID == -1) {
			sourceID = audio.obtainSource(true);
			if (sourceID == -1) return;
			if (buffers == null) {
				buffers = BufferUtils.createIntBuffer(bufferCount);
				alGenBuffers(buffers);
				if (alGetError() != AL_NO_ERROR) throw new GdxRuntimeException("Unabe to allocate audio buffers.");
			}
			alSourcei(sourceID, AL_LOOPING, AL_FALSE);
			alSourcef(sourceID, AL_GAIN, volume);
			// Queue empty buffers.
			tempBuffer.clear().flip();
			for (int i = 0; i < bufferCount; i++) {
				int bufferID = buffers.get(i);
				alBufferData(bufferID, format, tempBuffer, sampleRate);
				alSourceQueueBuffers(sourceID, bufferID);
			}
			alSourcePlay(sourceID);
			isPlaying = true;
			return;
		}

		while (length > 0) {
			int written = fillBuffer(data, offset, length);
			length -= written;
			offset += written;
		}
	}

	/** Blocks until some of the data could be buffered. */
	private int fillBuffer (byte[] data, int offset, int length) {
		int written = Math.min(bufferSize, length);

		outer:
		while (true) {
			int buffers = alGetSourcei(sourceID, AL_BUFFERS_PROCESSED);
			while (buffers-- > 0) {
				int bufferID = alSourceUnqueueBuffers(sourceID);
				if (bufferID == AL_INVALID_VALUE) break;
				renderedSeconds += secondsPerBuffer;

				tempBuffer.clear();
				tempBuffer.put(data, offset, written).flip();
				alBufferData(bufferID, format, tempBuffer, sampleRate);

				alSourceQueueBuffers(sourceID, bufferID);
				break outer;
			}
			// Wait for buffer to be free.
			try {
				Thread.sleep((long)(1000 * secondsPerBuffer / bufferCount));
			} catch (InterruptedException ignored) {
			}
		}

		// A buffer underflow will cause the source to stop.
		if (!isPlaying || alGetSourcei(sourceID, AL_SOURCE_STATE) != AL_PLAYING) {
			System.out.println("underflow!");
			alSourcePlay(sourceID);
			isPlaying = true;
		}

		return written;
	}

	public void stop () {
		if (sourceID == -1) return;
		audio.freeSource(sourceID);
		sourceID = -1;
		renderedSeconds = 0;
		isPlaying = false;
	}

	public boolean isPlaying () {
		if (sourceID == -1) return false;
		return isPlaying;
	}

	public void setVolume (float volume) {
		this.volume = volume;
		if (sourceID != -1) alSourcef(sourceID, AL_GAIN, volume);
	}

	public float getPosition () {
		if (sourceID == -1) return 0;
		return renderedSeconds + alGetSourcef(sourceID, AL11.AL_SEC_OFFSET);
	}

	public int getChannels () {
		return format == AL_FORMAT_STEREO16 ? 2 : 1;
	}

	public int getRate () {
		return sampleRate;
	}

	public void dispose () {
		if (buffers == null) return;
		if (sourceID != -1) {
			audio.freeSource(sourceID);
			sourceID = -1;
		}
		alDeleteBuffers(buffers);
		buffers = null;
	}

	public boolean isMono () {
		return false;
	}

	public int getLatency () {
		return (int)(secondsPerBuffer * bufferCount * 1000);
	}
}