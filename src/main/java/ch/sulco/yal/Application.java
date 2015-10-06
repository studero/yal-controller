package ch.sulco.yal;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import ch.sulco.yal.controller.MidiControl;
import ch.sulco.yal.dm.Channel;
import ch.sulco.yal.dsp.AppConfig;
import ch.sulco.yal.dsp.DataStore;
import ch.sulco.yal.dsp.audio.Processor;
import ch.sulco.yal.dsp.audio.onboard.AudioSystemProvider;
import ch.sulco.yal.event.ChannelCreated;
import ch.sulco.yal.event.EventManager;
import ch.sulco.yal.web.Server;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Singleton
public class Application {

	private final static Logger log = Logger.getLogger(Application.class.getName());

	@Inject
	private AppConfig appConfig;

	@Inject
	private Server server;

	@Inject
	private Processor audioProcessor;

	@Inject
	private MidiControl midiControl;

	@Inject
	private DataStore dataStore;

	@Inject
	private EventManager eventManager;

	@Inject
	private AudioSystemProvider audioSystemProvider;

	public void start() {
		for (Channel channel : this.audioSystemProvider.getChannels()) {
			this.dataStore.addChannel(channel);
			this.eventManager.addEvent(new ChannelCreated(channel));
		}
	}

	public Processor getAudioProcessor() {
		return this.audioProcessor;
	}

	public static Injector injector = Guice.createInjector(new YalModule(), new PostConstructModule());

	public static void main(String[] args) {
		Application application = injector.getInstance(Application.class);
		application.start();
	}
}
