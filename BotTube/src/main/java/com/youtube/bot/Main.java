package com.youtube.bot;

import com.sun.net.httpserver.HttpServer;
import com.xcoder.easyyt.EasyYoutube;
import com.xcoder.easyyt.ProgressListener;
import com.xcoder.easyyt.Project;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JProgressBar;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Main.exit(e instanceof InputMismatchException ? "Please enter a valid option" : "Invalid input, Expected a number and got a text"));
        var intro = """
                 ______      ____     ________   ________   __    __   ______     _____ \s
                (_   _ \\    / __ \\   (___  ___) (___  ___)  ) )  ( (  (_   _ \\   / ___/ \s
                  ) (_) )  / /  \\ \\      ) )        ) )    ( (    ) )   ) (_) ) ( (__   \s
                  \\   _/  ( ()  () )    ( (        ( (      ) )  ( (    \\   _/   ) __)  \s
                  /  _ \\  ( ()  () )     ) )        ) )    ( (    ) )   /  _ \\  ( (     \s
                 _) (_) )  \\ \\__/ /     ( (        ( (      ) \\__/ (   _) (_) )  \\ \\___ \s
                (______/    \\____/      /__\\       /__\\     \\______/  (______/    \\____\\\s
                                                                              Version : 1.0
                                                                              By ~ Rahil khan
                    """;

        var token = Preferences.userRoot().get("token", null);
        var id = Preferences.userRoot().get("id", null);
        var secret = Preferences.userRoot().get("secret", null);

        if (token == null || id == null || secret == null)
            authenticate();

        var original = System.err;
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        EasyYoutube youtube = new EasyYoutube(new Project(id, secret, token));
        System.setErr(original);
        System.out.println(intro);
        System.out.println("""
                Choose any option :-
                [1] Spam comments
                [2] Reply top 100 comments
                [3] Reply conditional comments
                [4] Search and comment (BotSurfing)
                [5] Logout
                [0] About/Help
                """);

        System.out.print("Enter your choice : ");
        var option = Integer.parseInt(scanner.nextLine());
        String video = "";
        if (option != 5 && option != 4 && option != 0) {
            System.out.print("Enter the video link : ");
            video = scanner.nextLine();
            if (video.contains("watch?v=")) {
                if (video.contains("&"))
                    video = video.substring(video.indexOf("=") + 1, video.indexOf("&"));
                else
                    video = video.substring(video.indexOf("=") + 1);
            } else
                exit("Please enter a valid video link");
        }
        switch (option) {
            case 1 -> {
                System.out.print("Enter comment : ");
                var comment = scanner.nextLine();
                System.out.print("Enter comment count (<200) : ");
                var count = Integer.parseInt(scanner.nextLine());
                ProgressBar progressBar = new ProgressBar("Commenting...", count);
                for (int i = 0; i < count; i++) {
                    try {
                        youtube.comment(video, comment).getResult(5);
                        progressBar.step();
                    } catch (Exception e) {
                        if (e instanceof EmptyStackException) {
                            System.out.println("Your quota of all accounts is exhausted, Exiting...");
                            System.exit(403);
                        } else
                            System.out.println("Failed to comment on a video");
                    }
                }
                progressBar.close();
                main(args);
            }
            case 2 -> {
                System.out.print("Enter comment : ");
                var comment = scanner.nextLine();
                var bar = new ProgressBar("Replying...", 100);
                youtube.replyAll(video, comment, new ProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        bar.stepTo(progress);
                        if (progress == 100) {
                            bar.close();
                            try {
                                main(args);
                            } catch (GeneralSecurityException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e instanceof EmptyStackException) {
                            System.out.println("Your quota of all accounts is exhausted, Exiting...");
                            System.exit(403);
                        } else
                            System.out.println("Failed to reply");
                    }
                });
            }
            case 3 -> {
                System.out.println("""
                        Choose any one condition :-
                        [1] Where comments contains something
                        [2] Where comments starts with something
                        """);
                System.out.print("Enter your choice :- ");
                var condition = Integer.parseInt(scanner.nextLine());
                System.out.print("Enter the conditional word : ");
                var word = scanner.nextLine();
                System.out.print("Enter reply : ");
                var reply = scanner.nextLine();

                ProgressBar bar = new ProgressBarBuilder()
                        .setStyle(ProgressBarStyle.ASCII)
                        .setTaskName("Replying comments on the basis of your condition...")
                        .build();
                Predicate<String> predicate = condition == 1 ? s -> s.contains(word) : s -> s.startsWith(word);
                youtube.replyMatching(video, reply, predicate, new ProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        bar.stepTo(progress);
                        if (progress == 100) {
                            try {
                                bar.close();
                                main(args);
                            } catch (GeneralSecurityException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e instanceof EmptyStackException) {
                            System.out.println("Your quota of all accounts is exhausted, Exiting...");
                            System.exit(403);
                        } else
                            System.out.println("Failed to reply");
                    }
                });
            }
            case 4 -> {
                System.out.print("Enter comma separated keywords : ");
                var keywords = scanner.nextLine().split(",");
                System.out.print("Enter comment : ");
                var comment = scanner.nextLine();
                ProgressBar bar = new ProgressBar("Commenting", keywords.length * 10L);
                for (var keyword : keywords) {
                    try {
                        String[] result = youtube.search(keyword, EasyYoutube.FILTER_LATEST_VIRAL).getResult(5);
                        for (var videoId : result) {
                            youtube.comment(videoId, comment).getResult(5);
                            bar.step();
                            bar.setExtraMessage("Commented on " + videoId);
                        }
                    } catch (Exception e) {
                        if (e instanceof EmptyStackException) {
                            System.out.println("Your quota of all accounts is exhausted, Exiting...");
                            System.exit(403);
                        } else
                            System.out.println("Failed to comment on a video, Skipping...");
                    }
                }
                bar.close();
                main(args);
            }
            case 0 -> {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/ErrorxCode/BotTube"));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                } else
                    System.out.println("Visit https://github.com/ErrorxCode/BotTube for more information");
            }
            case 5 -> {
                Preferences.userRoot().remove("token");
                System.out.println("Logged out successfully");
                System.exit(200);
            }
        }
    }

    private static void authenticate() {
        System.out.print("Enter your client id : ");
        var clientId = scanner.nextLine();
        System.out.print("Enter your client secret : ");
        var clientSecret = scanner.nextLine();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            System.out.println("Waiting for authentication...");
            try {
                HttpServer.create(new InetSocketAddress(80), 0).createContext("/", exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    var code = query.substring(query.indexOf("=") + 1, query.indexOf("&"));
                    System.out.println(code);
                    var message = "<h1>Successfully authenticated. You can go back to the CLI</h1>";
                    exchange.sendResponseHeaders(200, message.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(message.getBytes());
                    os.close();

                    System.out.println("Authentication...");
                    Map<String, String> params = new HashMap<>();
                    params.put("grant_type", "authorization_code");
                    params.put("code", code);
                    params.put("redirect_uri", "http://127.0.0.1");
                    params.put("client_id", clientId);
                    params.put("client_secret", clientSecret);

                    String form = params.entrySet()
                            .stream()
                            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                            .collect(Collectors.joining("&"));

                    HttpRequest request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
                            .setHeader("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
                    try {
                        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.statusCode() == 200) {
                            response.body().lines().forEach(line -> {
                                if (line.contains("refresh_token")) {
                                    var token = line.substring(line.indexOf("1//"), line.length() - 2);
                                    System.out.println(token);
                                    Preferences root = Preferences.userRoot();
                                    root.put("id", clientId);
                                    root.put("secret", clientSecret);
                                    root.put("token", token);
                                    synchronized (scanner) {
                                        exchange.close();
                                        scanner.notify();
                                    }
                                }
                            });
                        } else {
                            exit("Error getting access token. Error details :-\n" + response.body());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).getServer().start();
                Desktop.getDesktop().browse(new URI("https://accounts.google.com/o/oauth2/v2/auth?scope=https://www.googleapis.com/auth/youtube.force-ssl&response_type=code&access_type=offline&redirect_uri=http://127.0.0.1&client_id=" + clientId));
                synchronized (scanner) {
                    scanner.wait();
                    clear();
                }
            } catch (IOException | URISyntaxException | InterruptedException e) {
                exit("Something went wrong while fetching access_token from google servers.");
            }
        }
    }

    private static void type(String message) {
        char[] chars = message.toCharArray();
        for (char c : chars) {
            System.out.print(c);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void clear() {
        try {
            if (Objects.requireNonNull(System.getProperty("os.name")).contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                Runtime.getRuntime().exec("clear");

        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static void exit(String message) {
        try {
            System.out.println(message + " (Press enter to exit)");
            System.in.read();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}