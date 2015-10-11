package ch.sulco.yal.dsp.audio;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sulco.yal.dm.InputChannel;
import ch.sulco.yal.dm.RecordingState;
import ch.sulco.yal.dsp.audio.onboard.LoopListener;
import ch.sulco.yal.dsp.audio.onboard.LoopStore;
import ch.sulco.yal.dsp.audio.onboard.Synchronizer;
import ch.sulco.yal.event.ChannelUpdated;
import ch.sulco.yal.event.EventManager;

public abstract class AudioSource implements LoopListener, AudioDataListener {

	private final static Logger log = LoggerFactory.getLogger(AudioSource.class);

	@Inject
	private Synchronizer synchronizer;

	@Inject
	private LoopStore loopStore;

	@Inject
	private EventManager eventManager;

	private InputChannel inputChannel;

	private byte[] recordedSample;
	private ByteArrayOutputStream recordingSample;
	// private ByteArrayOutputStream monitoringSample;

	private List<AudioDataListener> audioDataListeners = new ArrayList<>();

	public void addAudioDataListener(AudioDataListener audioDataListener) {
		this.audioDataListeners.add(audioDataListener);
	}

	protected void triggerNewAudioData(byte[] data) {
		for (AudioDataListener audioDataListener : this.audioDataListeners) {
			audioDataListener.newAudioData(data);
		}
	}

	protected InputChannel getInputChannel() {
		return this.inputChannel;
	}

	public void initialize() {
		this.addAudioDataListener(this);
		this.setRecordingState(RecordingState.STOPPED);

	}

	public void setMonitoring(boolean monitoring) {
		this.inputChannel.setMonitoring(monitoring);
		this.eventManager.addEvent(new ChannelUpdated(this.inputChannel));
	}

	public void setInputChannel(InputChannel inputChannel) {
		this.inputChannel = inputChannel;
	}

	public RecordingState getRecordingState() {
		return this.inputChannel.getRecordingState();
	}

	public void startRecord() {
		if (this.inputChannel.getRecordingState() == RecordingState.STOPPED) {
			this.setRecordingState(RecordingState.WAITING);
			this.synchronizer.addLoopListerner(this);
		}
	}

	public void stopRecord() {
		this.setRecordingState(RecordingState.STOPPED);
		this.synchronizer.removeLoopListerner(this);
		if (!this.inputChannel.isOverdubbing()) {
			this.recordedSample = this.recordingSample.toByteArray();
		}
		if (this.recordedSample != null) {
			this.loopStore.addSample(this.recordedSample);
			this.recordedSample = null;
			this.recordingSample = null;
		}
	}

	public boolean isMonitoring() {
		return this.inputChannel.isMonitoring();
	}

	@Override
	public boolean isRecorder() {
		return true;
	}

	protected abstract Thread getRecordThread();

	@Override
	public void loopStarted(boolean firstLoop) {
		log.info("Loop Started [firstLoop=" + firstLoop + "]");
		if (this.inputChannel.getRecordingState() == RecordingState.WAITING) {
			this.inputChannel.setOverdubbing(!firstLoop);
			this.setRecordingState(RecordingState.RECORDING);
			this.recordedSample = null;
			this.recordingSample = new ByteArrayOutputStream();
			this.getRecordThread().start();
		} else if (this.inputChannel.getRecordingState() == RecordingState.RECORDING) {
			this.recordedSample = this.recordingSample.toByteArray();
			this.recordingSample = new ByteArrayOutputStream();
		} else {
			this.synchronizer.removeLoopListerner(this);
		}
	}

	@Override
	public void newAudioData(byte[] data) {
		if (this.getInputChannel().getRecordingState() == RecordingState.RECORDING) {
			if (this.recordingSample == null) {
				this.recordingSample = new ByteArrayOutputStream();
			}
			this.recordingSample.write(data, 0, data.length);
		}
	}

	private void setRecordingState(RecordingState recordingState) {
		log.info("Change RecordingState [" + this.inputChannel.getId() + "][" + recordingState + "]");
		this.inputChannel.setRecordingState(recordingState);
		this.eventManager.addEvent(new ChannelUpdated(this.inputChannel));
	}

	// private void updateMonitoring(int monitoringCount, byte[] buffer, int
	// bytesRead) {
	// if (Recorder.this.inputChannel.isMonitoring()) {
	// if (Recorder.this.monitoringSample == null)
	// Recorder.this.monitoringSample = new ByteArrayOutputStream();
	// Recorder.this.monitoringSample.write(buffer, 0, bytesRead);
	// monitoringCount++;
	// int aggregation = 60000; // 2000;
	// if (monitoringCount % aggregation == 0) {
	// byte[] byteArray = Recorder.this.monitoringSample.toByteArray();
	// float[] samples = new float[byteArray.length / 2];
	// for (int i = 0; i < byteArray.length; i += 2) {
	// byte b1 = byteArray[i];
	// byte b2 = byteArray[i + 1];
	// if (Recorder.this.appConfig.getAudioFormat().isBigEndian()) {
	// samples[i / 2] = (b1 << 8 | b2 & 0xFF) / 32768f;
	// } else {
	// samples[i / 2] = (b2 << 8 | b1 & 0xFF) / 32768f;
	// }
	// }
	// double value = 0;
	// for (float sample : samples) {
	// value += sample * sample;
	// }
	// float rms = (float) Math.sqrt(value / (samples.length));
	// Recorder.this.inputChannel.setLevel(rms);
	// Recorder.this.eventManager.addEvent(new
	// ChannelUpdated(Recorder.this.inputChannel));
	// monitoringCount = 0;
	// Recorder.this.monitoringSample = null;
	// }
	// } else {
	// Recorder.this.monitoringSample = null;
	// }
	// }
}
