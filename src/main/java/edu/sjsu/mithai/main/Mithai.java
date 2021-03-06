package edu.sjsu.mithai.main;

import edu.sjsu.mithai.config.ConfigFileObservable;
import edu.sjsu.mithai.config.Configuration;
import edu.sjsu.mithai.config.MithaiProperties;
import edu.sjsu.mithai.data.DataGenerationTask;
import edu.sjsu.mithai.data.MetadataGenerationTask;
import edu.sjsu.mithai.data.SensorStore;
import edu.sjsu.mithai.export.ExporterTask;
import edu.sjsu.mithai.export.HttpExporterTask;
import edu.sjsu.mithai.mqtt.MQTTDataReceiverTask;
import edu.sjsu.mithai.mqtt.MQTTMetaDataRecieverTask;
import edu.sjsu.mithai.mqtt.MQTTPublisherTask;
import edu.sjsu.mithai.sensors.TemperatureSensor;
import edu.sjsu.mithai.spark.SparkStreamingObject;
import edu.sjsu.mithai.spark.Store;
import edu.sjsu.mithai.util.TaskManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import static edu.sjsu.mithai.config.MithaiProperties.EXPORTER_TYPE;
import static edu.sjsu.mithai.config.MithaiProperties.NUMBER_OF_SENSORS;

public class Mithai implements Observer {

    protected static Configuration configuration;
    protected SensorStore sensorStore;


    public static void main(String[] args) throws Exception {
        Mithai mithai = new Mithai();
        if(args.length<1)
            mithai.start(null);
        else
            mithai.start(args[0]);
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    protected void start(String arg) throws Exception {

        Logger.getLogger("org").setLevel(Level.ERROR);
        Logger.getLogger("akka").setLevel(Level.ERROR);

        ConfigFileObservable.getInstance().addObserver(this);

        Runtime.getRuntime().addShutdownHook(new ShutDownHook());

        //TODO file path will be provided by user
        if (arg == null || arg.equals("")) {
            File configFile = new File(Mithai.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            configuration = new Configuration(configFile.getParent() + "/application.properties");
        } else
            configuration = new Configuration(arg);

        sensorStore = new SensorStore();

        loadDevices();

        setupHandlers();

        boolean receiverTask = false;
        //Start tasks here
//        TaskManager.getInstance().submitTask(new ConfigMonitorTask(configuration));

        String type = configuration.getProperty(MithaiProperties.TASK_LIST);
        String[] tasks = type.split(",");

        for (String task : tasks) {
            task = task.trim();
            if (task.equals("MQTTDataReceiverTask")) {
                TaskManager.getInstance().submitTask(new MQTTDataReceiverTask(configuration));
                receiverTask = true;
            } else if (task.equals("MQTTMetaDataRecieverTask")) {
                TaskManager.getInstance().submitTask(new MQTTMetaDataRecieverTask(configuration));
                receiverTask = true;
            }
            if (task.equals("MQTTPublisherTask")) {
                TaskManager.getInstance().submitTask(new MQTTPublisherTask(configuration));
            }
            if (task.equals("DataGenerationTask")) {
                TaskManager.getInstance().submitTask(new DataGenerationTask(configuration, sensorStore));
            } else if (task.equals("MetadataGenerationTask")) {
                TaskManager.getInstance().submitTask(new MetadataGenerationTask(configuration));
            }

        }
        TaskManager.getInstance().submitTask(new HttpExporterTask(configuration));

        if (!configuration.getProperty(EXPORTER_TYPE).equals("HTTP")) {
            TaskManager.getInstance().submitTask(new ExporterTask(configuration, Store.messageStore()));
        }

        // Start Streaming context

        Thread.sleep(Long.parseLong(configuration.getProperty(MithaiProperties.STARTUP_THRESHOLD)) * 1000);
        if (receiverTask) {
            SparkStreamingObject.streamingContext().start();
        }
//        // Stop all tasks and wait 60 seconds to finish them
//        TaskManager.getInstance().stopAll();
    }

    protected synchronized void loadDevices() {
        sensorStore.getDevices().clear();

        for (int i = 1; i<= Integer.parseInt(configuration.getProperty(NUMBER_OF_SENSORS)); i++) {
            sensorStore.addDevice(new TemperatureSensor("sensor" + i));
        }
    }

    protected synchronized void setupHandlers() {
        TaskManager.getInstance().addHandler(new MithaiHandler());
    }

    @Override
    public void update(Observable observable, Object o) {

        if (observable instanceof ConfigFileObservable) {
            loadDevices();

            // Kick out old data generation task and start new one
            TaskManager.getInstance().stop(DataGenerationTask.class);

            try {
                TaskManager.getInstance().submitTask(new DataGenerationTask(configuration, sensorStore));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ShutDownHook extends Thread {

        @Override
        public void run() {
            System.out.println("###Shutdown triggered.. Stopping all tasks..");
            try {
                TaskManager.getInstance().stopAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
