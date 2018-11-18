package com.atronandbeyond;

import com.atronandbeyond.domains.ImageResults;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class Downloader {
    private Logger logger;
    private Config config;
    private OkHttpClient okHttpClient;
    private Gson gson;
    Downloader() {
        config = new Config();
        logger = Logger.getLogger(getClass().getSimpleName());
        okHttpClient = new OkHttpClient();
        gson = new Gson();
    }
    public List<String> getImagesForCityState(String cityState) {
        String cityStateCleaned = cityState.replace(" ", "+");
        StringBuilder sb = new StringBuilder();
        sb.append("https://www.googleapis.com/customsearch/v1?key=")
                .append(config.getApi())
                .append("&cx=")
                .append(config.getCx())
                .append("&searchType=image")
                .append("&q=")
                .append(cityStateCleaned);
        Request request = new Request.Builder()
                .url(sb.toString())
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            String json = response.body().string();
            ImageResults imageResults = gson.fromJson(json, ImageResults.class);
            for (ImageResults.Items item : imageResults.getItems()) {
                if (item.getImage().getWidth() >= Integer.valueOf(config.getMinImageSize())) {
                    String filename = item.getLink().substring(item.getLink().lastIndexOf("/")+1, item.getLink().lastIndexOf("."));
                    String filepath = config.getMediaDirectory() + File.separator + cityStateCleaned + File.separator + filename;
                    logger.info(item.toString());
                    // check if dir exist
                    logger.info("filename: " + filename);
                    if (!Files.exists(Paths.get(config.getMediaDirectory() + File.separator + cityStateCleaned))) {
                        Files.createDirectories(Paths.get(config.getMediaDirectory() + File.separator + cityStateCleaned));
                    }
                    // download images
                    if (!Files.exists(Paths.get(filepath))) {
                        URL url = new URL(item.getLink());
                        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                        FileOutputStream fos = new FileOutputStream(filepath);
                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    }
                }
            }

            logger.info(imageResults.getItems().toString());
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
        return null;
    }
}