package main;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import db.Data;
import gui.AnimationUtils;
import gui.controllers.MainScreenController;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class YTConnector extends Task {

    MainScreenController MainScreenController;


    @Override
    protected Object call() throws IOException {

        boolean initialization = true;

        //inicjalizacja usługi youtube
        YouTube youtubeService = null;
        try {
            youtubeService = Library.getService();
        } catch (GeneralSecurityException | IOException e1) {
            e1.printStackTrace();
        }

        //stwórz i wyślij żądanienie informacji o kanale użytkownika
        Channel channel = getChannel(youtubeService);

        //uzyskiwanie danych dotyczących listy uploadów
        String uploadsPlaylist = channel.getContentDetails().getRelatedPlaylists().getUploads();

        YouTube.PlaylistItems.List playlistItemRequest = null;
        playlistItemRequest = youtubeService.playlistItems().list("id,contentDetails,snippet");

        playlistItemRequest.setPlaylistId(uploadsPlaylist);

        playlistItemRequest.setFields("items(contentDetails/videoId,snippet/title" +
                ",snippet/publishedAt,snippet/resourceId/videoId),nextPageToken,pageInfo");

        List<PlaylistItem> playlistItemList = new ArrayList<PlaylistItem>();
        List<String> videosIDs = new ArrayList<String>();

        LocalDate datapoczatkowa = Data.getCurrentConfiguration().getBegining_date();
        DateTime dataPublikacji;
        LocalDate data;


        //pobieranie spełniających kryteria filmów
        logText("Pobieranie listy wrzuconych, przed zadaną datą, filmów... ");

        try {
            getVideosFromPlaylist(playlistItemRequest, playlistItemList, datapoczatkowa);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        setProgress(40);

        //tworzenie kolekcji filmów spełniających waunki
        filterVideos(playlistItemList, videosIDs, datapoczatkowa);

        setProgress(60);

        if (videosIDs.size() == 0) {
            throwErrorAndGetBackToMainScreen("Nie znaleziono pasujących filmów. Spróbuj zmienić datę");
        }

        //stworzenie stringa zawierajacego ID filmów po przecinku, potrzebne do wysłania jednego requesta, zamiast kilkunastu
        ArrayList<StringBuilder> stringRequests = buildRequests(videosIDs);
        logText("Utworzono requesty.");

        List<Video> videosList = new ArrayList<Video>();
        ArrayList<VideoListResponse> APIResponses = new ArrayList<VideoListResponse>();

        executeRequests(youtubeService, stringRequests, videosList, APIResponses);

        logText("Pobrano " + videosList.size() + " filmów");
        setProgress(80);

        logText(videosList.get(0).getSnippet().getTitle());

        BigInteger likesAmount;
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        DecimalFormat format = new DecimalFormat("0.0#", otherSymbols);

        int liczbaLapekWGore = 0;

        loop(initialization, youtubeService, stringRequests, videosList, APIResponses, format, liczbaLapekWGore);

        return null;
    }


    private void loop(boolean initialization, YouTube youtubeService, ArrayList<StringBuilder> stringRequests, List<Video> videosList, ArrayList<VideoListResponse> APIResponses, DecimalFormat format, int liczbaLapekWGore) {

        BigInteger likesAmount;

        while (true) {

            for (Video video : videosList) {

                logText("Tytuł wideo: " + video.getSnippet().getTitle());
                logText("Liczba lapek w góre: " + video.getStatistics().getLikeCount());
                likesAmount = video.getStatistics().getLikeCount();
                liczbaLapekWGore = liczbaLapekWGore + likesAmount.intValue();
                //
            }

            logText("-----------------------");
            logText("Wynik:");
            logText("Liczba filmow: " + videosList.size());
            logText("Liczba lapek w góre w sumie: " + liczbaLapekWGore);
            logText("Zebrana kwota: " + format.format(Double.valueOf(BigDecimal.valueOf(liczbaLapekWGore).multiply(
                    BigDecimal.valueOf(Double.valueOf(Data.getCurrentConfiguration().getPrzelicznik()))).toString())) + " zł");
            logText("Ustawiono widok.");

            setProgress(100);

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String currentPlnValue = MainScreenController.getPlnSum().getText().replaceFirst(" zł", "");
            System.out.println(currentPlnValue);
            Double currentPlnValueDouble = Double.valueOf(currentPlnValue);
            try {
                System.out.println("Wartość: " + Double.valueOf(currentPlnValue));
                currentPlnValueDouble = Double.valueOf(currentPlnValue);
            } catch (NumberFormatException e) {
                System.out.println("Błąd");
            }

            System.out.println(format.format(liczbaLapekWGore * Double.parseDouble(Data.getCurrentConfiguration().getPrzelicznik())));
            System.out.println(MainScreenController.getThumbsUpSum().getText().replaceFirst(" łapek!", ""));
            int currentThumbsUpNumber = (int) Integer.valueOf(MainScreenController.getThumbsUpSum().getText().replaceFirst(" łapek!", ""));

            if (initialization) {
                MainScreenController.setPlnSum(Double.valueOf(format.format(liczbaLapekWGore * Double.valueOf(Data.getCurrentConfiguration().getPrzelicznik()))).toString() + " zł");
                MainScreenController.setThumbsUpSum(liczbaLapekWGore + " łapek!");
            } else {

				try {
					AnimationUtils.animateUpPLN(currentPlnValueDouble, Double.parseDouble(BigDecimal.valueOf(liczbaLapekWGore)
							.multiply(BigDecimal.valueOf(Double.parseDouble(Data.getCurrentConfiguration().getPrzelicznik()))).toString()));
					AnimationUtils.animateUpThumbsUp(currentThumbsUpNumber, liczbaLapekWGore);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

			}

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			logText("Wykonuje request ponownie...");
            setProgress(0);
            liczbaLapekWGore = 0;
            initialization = false;
            APIResponses.clear();
            videosList.clear();

			try {
				invokeRequests(youtubeService, stringRequests, videosList, APIResponses);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
    }

    private void executeRequests(YouTube youtubeService, ArrayList<StringBuilder> stringRequests, List<Video> videosList, ArrayList<VideoListResponse> APIResponses) {
        for (int i = 0; i < stringRequests.size(); i++) {

			try {
				APIResponses.add(youtubeService.videos().list("id, statistics,snippet")
						.setId(stringRequests.get(i).toString()).execute());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			videosList.addAll(APIResponses.get(i).getItems());
            logText("Wykonano request " + i + " z " + stringRequests.size());

        }
    }

    private void filterVideos(List<PlaylistItem> playlistItemList, List<String> videosIDs, LocalDate datapoczatkowa) {
        LocalDate data;
        DateTime dataPublikacji;
        for (int i = 0; i < playlistItemList.size(); i++) {

            //znajdz date publikacji analizowanego filmu
            dataPublikacji = playlistItemList.get(i).getSnippet().getPublishedAt();
            data = Instant.ofEpochMilli(dataPublikacji.getValue()).atZone(ZoneId.systemDefault()).toLocalDate();

            if (data.isAfter(datapoczatkowa)) {
                logText(playlistItemList.get(i).getSnippet().getTitle() + " wrzucony " + data);
                System.out.println("Dodaje do listy: " + playlistItemList.get(i).getSnippet().getTitle());
                videosIDs.add(playlistItemList.get(i).getSnippet().getResourceId().getVideoId());
            }
        }
    }

    private void invokeRequests(YouTube youtubeService, ArrayList<StringBuilder> stringRequests, List<Video> videosList, ArrayList<VideoListResponse> APIResponses) throws IOException {
		for (int i = 0; i < stringRequests.size(); i++) {

			APIResponses.add(youtubeService.videos().list("id, statistics,snippet")
					.setId(stringRequests.get(i).toString()).execute());
			videosList.addAll(APIResponses.get(i).getItems());

		}
	}

	private ArrayList<StringBuilder> buildRequests(List<String> videosIDs) {
		ArrayList<StringBuilder> stringRequests = new ArrayList<StringBuilder>();

        System.out.println(Double.valueOf(videosIDs.size() / 49));
		int numberOfRequests = (int) Math.ceil(Double.valueOf(videosIDs.size()) / 49);

		logText("Liczba zaptyań: " + numberOfRequests);
		logText("Pobrano: " + videosIDs.size());

		int remainingRequests = videosIDs.size();
		int videosIDsCounter = 0;
		int stringBuilderCounter = 0;

		logText("Tworzenie requesta...");
		while (remainingRequests != 0) {

			stringRequests.add(new StringBuilder());

			int currentRequestSize;

			if (remainingRequests % 49 != 0) {

				currentRequestSize = remainingRequests % 49;
			} else {
				currentRequestSize = 49;
			}

			for (int i = 0; i < currentRequestSize; i++) {
				if (i == (1 - videosIDs.size())) {
					stringRequests.get(stringBuilderCounter).append(videosIDs.get(videosIDsCounter));
				} else {
					stringRequests.get(stringBuilderCounter).append(videosIDs.get(videosIDsCounter));
					stringRequests.get(stringBuilderCounter).append(",");
				}
				videosIDsCounter++;
			}

			remainingRequests = remainingRequests - currentRequestSize;

			stringBuilderCounter++;

		}
		return stringRequests;
	}

	private void getVideosFromPlaylist(YouTube.PlaylistItems.List playlistItemRequest, List<PlaylistItem> playlistItemList, LocalDate datapoczatkowa) throws IOException {
		String nextToken = "";
		int temp = 0;
		do {
			playlistItemRequest.setPageToken(nextToken);
			PlaylistItemListResponse playlistItemResult = playlistItemRequest.execute();

			playlistItemList.addAll(playlistItemResult.getItems());

			nextToken = playlistItemResult.getNextPageToken();
			System.out.append(".");
			System.out.println(playlistItemList.get(temp).getSnippet().getPublishedAt());

			if (Instant.ofEpochMilli(playlistItemList.get(temp).getSnippet().getPublishedAt().getValue()).atZone(ZoneId.systemDefault()).toLocalDate()
					.isBefore(datapoczatkowa)) {

				break;

			}
			temp = temp + playlistItemResult.getItems().size();


		} while (nextToken != null);
	}

	private Channel getChannel(YouTube youtubeService) throws IOException {
        YouTube.Channels.List channelsListByUsernameRequest = null;
            channelsListByUsernameRequest = youtubeService.channels().list("snippet,statistics, contentDetails");


        channelsListByUsernameRequest.setId(Data.getCurrentConfiguration().getChannel_id());

        ChannelListResponse response = null;
            response = channelsListByUsernameRequest.execute();


        Channel channel = response.getItems().get(0);
        logText("Znaleziono kanał " + channel.getSnippet().getTitle());
        setProgress(10);
        return channel;
    }


    public MainScreenController getMainScreenController() {
        return MainScreenController;
    }

    public void setMainScreenController(MainScreenController mainScreenController) {
        MainScreenController = mainScreenController;
    }

    public void throwErrorAndGetBackToMainScreen(String message) {

        Platform.runLater(new Runnable() {

            @Override
            public void run() {

                try {
                    MainScreenController.throwErrorAndGetBackToMainScreen(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        });
    }

    public void logText(String text) {
        Platform.runLater(() -> {
            Data.getLoadingController().getTextArea().appendText(LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)) + ": " + text + "\n");
        });


    }

    public void setProgress(double num) {
        Data.getLoadingController().getProgressBar().setProgress(num);
    }

}
