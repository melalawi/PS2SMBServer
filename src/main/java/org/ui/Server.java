package org.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.filesys.app.SMBFileServer;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class Server extends Application {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 500;
    private static final String DEFAULT_CONTROL_INNER_BACKGROUND = "derive(white,80%)";
    private static final String HIGHLIGHTED_CONTROL_INNER_BACKGROUND = "derive(blue, 50%)";

    private static final String QUESTION = "?";
    private static final String CHECK = "✔";
    private static final String ERROR = "❌";

    private static File ps2LoadedROMTextFile;
    private static Configuration configuration;

    public static List<String> ps2LoadedROMList = new ArrayList<>();
    private static List<String> pcROMList = new ArrayList<>();

    private static ListView<String> pcROMListView = new ListView<>();
    private ImageView coverImage;
    private Button startPs2ServerButton;
    private Text statusLabelText;

    private static GameMetadata gameMetadata;
    private static ThreadPoolExecutor unzipPool;


    public static void main(String[] args) throws Exception {
        configuration = new Configuration();
        gameMetadata = new GameMetadata();
        unzipPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!Unzipper.ACTIVE_UNZIPS.isEmpty()) {
                    pcROMListView.refresh();
                }
            }
        }, 2000, 1000);

        ps2LoadedROMTextFile = new File(configuration.ps2ROMPath + File.separator + "PS2ROMS.txt");

        launch(args);
    }
    @Override
    public void stop() {
        unzipPool.shutdownNow();
        SMBFileServer.shutdownServer(null);
    }

    public static void startPS2Server() {
        SMBFileServer.main(new String[0]);
    }

    private Unzipper getUnzipper(final String name) {
        for (Unzipper unzipper : Unzipper.ACTIVE_UNZIPS) {
            if (unzipper.getFileName().equals(name)) {
                return unzipper;
            }
        }
        return null;
    }

    private void hookUps() {
        updateStatusLabel();
        resetPCRomListView();
        updatePS2PortedFiles();
        updateRomListView();

        updatePS2PortedFiles();

        pcROMListView.setOnMouseClicked(click -> {
            String selected = pcROMListView.getSelectionModel().getSelectedItem();

            if (selected == null) {
                return;
            }

            displaySelectedRom(selected);
            if (click.getClickCount() == 2) {
                portToPS2(selected);
            }
        });

        startPs2ServerButton.setOnAction(e -> {
            startPs2ServerButton.setDisable(true);
            new Thread(() -> Server.startPS2Server()).start();
        });
    }

    private void displaySelectedRom(final String game) {
        String selectedROMName = game.substring(0, game.lastIndexOf('(')).trim();

        String closestArtFile = gameMetadata.getClosestArtFile(selectedROMName, configuration.ps2ROMArtDirectory);

        Image ps2Image = new Image(closestArtFile);

        coverImage.setImage(ps2Image);
    }

    private void resetPCRomListView() {
        File pcROMPath = new File(configuration.pcROMPath);
        pcROMList.clear();

        if (pcROMPath.exists()) {
            String[] pcROMPathList = pcROMPath.list();

            for (String game : pcROMPathList) {
                pcROMList.add(game);
            }
        }

    }

    private void portToPS2(final String game) {
        if (ps2LoadedROMList.contains(game.trim())) {
            return;
        }
        System.out.println("clicked on " + game);



        Unzipper task = new Unzipper(configuration.pcROMPath, game.trim(), configuration.ps2ROMPath);

        unzipPool.execute(task);

        updatePS2TextFile();
        resetPCRomListView();
        updatePS2PortedFiles();
        updateRomListView();
    }

    private void updateRomListView() {
        List<String> finalList = new ArrayList<>();
        pcROMListView.getSelectionModel().clearSelection();

        finalList.addAll(pcROMList);

        for (String ps2ROM : ps2LoadedROMList) {
            if (!finalList.contains(ps2ROM)) {
                finalList.add(ps2ROM);
            }
        }

        Collections.sort(finalList);

        pcROMListView.setItems(FXCollections.observableArrayList(finalList));
    }

    private void updatePS2PortedFiles() {
        if (!ps2LoadedROMTextFile.exists()) {
            return;
        }

        try {
            FileReader reader = new FileReader(ps2LoadedROMTextFile);
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                if (!ps2LoadedROMList.contains(line)) {
                    ps2LoadedROMList.add(line);
                }
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePS2TextFile() {
        try {
            FileWriter writer = new FileWriter(ps2LoadedROMTextFile, false);
            BufferedWriter bufferedWriter = new BufferedWriter(writer);

            for (String game : ps2LoadedROMList) {
                bufferedWriter.write(game);
                bufferedWriter.newLine();
            }

            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatusLabel() {
        if (new File(configuration.pcROMPath).exists()) {
            statusLabelText.setText(CHECK);
        } else {
            statusLabelText.setText(ERROR);
            //statusLabelTooltip.setText("Path '" + configuration.pcROMPath + "' not found!");
        }

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("PS2 Game Loader");
        Group root = new Group();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setMinHeight(HEIGHT);
        primaryStage.setMinWidth(WIDTH);

        BorderPane borderPane = new BorderPane();
        HBox toolbar = generateToolbar();
        addQuestionMark(toolbar);

        borderPane.setTop(toolbar);
        borderPane.setLeft(generateLeftSection());
        borderPane.setRight(generateRightSection());
        borderPane.setCenter(generateMiddleSection());

        borderPane.prefHeightProperty().bind(scene.heightProperty());
        borderPane.prefWidthProperty().bind(scene.widthProperty());

        root.getChildren().add(borderPane);
        primaryStage.setScene(scene);

        primaryStage.getIcons().add(new Image(getClass().getResource("/rat.png").toString()));

        hookUps();
        primaryStage.show();
    }

    private HBox generateToolbar() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: #336699;");


        startPs2ServerButton = new Button("Start PS Server");
        startPs2ServerButton.setPrefSize(100, 20);


        hbox.getChildren().addAll(startPs2ServerButton);

        return hbox;
    }

    public void addQuestionMark(HBox hb) {
        StackPane stack = new StackPane();
        Rectangle helpIcon = new Rectangle(30.0, 25.0);
        helpIcon.setFill(
                new LinearGradient(0,0,0,1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#4977A3")),
                        new Stop(0.5, Color.web("#B0C6DA")),
                        new Stop(1,Color.web("#9CB6CF"))
                )
        );
        helpIcon.setStroke(Color.web("#D0E6FA"));
        helpIcon.setArcHeight(3.5);
        helpIcon.setArcWidth(3.5);

        statusLabelText = new Text(QUESTION);
        statusLabelText.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        statusLabelText.setFill(Color.WHITE);
        statusLabelText.setStroke(Color.web("#7080A0"));


        stack.getChildren().addAll(helpIcon, statusLabelText);
        stack.setAlignment(Pos.CENTER_RIGHT);     // Right-justify nodes in stack
        StackPane.setMargin(statusLabelText, new Insets(0, 5, 0, 5)); // Center "?"

        hb.getChildren().add(stack);            // Add to HBox from Example 1-2
        HBox.setHgrow(stack, Priority.ALWAYS);    // Give stack any extra space
    }
    public GridPane generateMiddleSection() {
        GridPane grid = new GridPane();

        coverImage = new ImageView();

        coverImage.setFitWidth(300);
        coverImage.setPreserveRatio(true);

        Image ps2Image = new Image(getClass().getResource("/PS2Blank.png").toString());

        coverImage.setImage(ps2Image);

        Group imageGroup = new Group();

        imageGroup.getChildren().add(coverImage);


        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        GridPane.setHalignment(coverImage, HPos.CENTER);
        GridPane.setValignment(coverImage, VPos.CENTER);


        grid.getChildren().add(imageGroup);

        return grid;
    }

    private MediaView leezard() {
        URL url = getClass().getResource("/leezard.mp4");

        Media media = new Media(url.toString());

        MediaPlayer player = new MediaPlayer(media);

        player.setRate(1.15);

        player.setOnEndOfMedia(() -> {
            player.seek(Duration.ZERO);
            player.play();
        });

        return new MediaView(player);
    }

    public VBox generateRightSection() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Text title = new Text("Logs");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        vbox.getChildren().add(title);

        TextArea logArea = new TextArea();
        logArea.setPrefSize(200, 1024);

        VBox.setVgrow(logArea, Priority.ALWAYS);

        MediaView leezard = leezard();

        leezard.setFitHeight(200);
        leezard.setFitWidth(200);

        vbox.getChildren().add(logArea);
        vbox.getChildren().add(leezard);

        leezard.getMediaPlayer().play();

        return vbox;
    }

    public VBox generateLeftSection() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        Text title = new Text("ROMs");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        vbox.getChildren().add(title);

        pcROMListView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item == null || empty) {
                            setText(null);
                            setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                        } else {
                            Unzipper unzipper = getUnzipper(item);
                            setText(item);

                            if (ps2LoadedROMList.contains(item)) {
                                setStyle("-fx-control-inner-background: " + HIGHLIGHTED_CONTROL_INNER_BACKGROUND + ";");
                            } else if (unzipper != null) {
                                int currentProgress = unzipper.progress.get();


                                setText(currentProgress + "% - " + item);

                                // Ensure progress always visible
                                if (currentProgress < 10) {
                                    currentProgress = 10;
                                }

                                setStyle(
                                        String.format(
                                                "-fx-background-color: linear-gradient(to right, %s 0%%, %s %d%%, -fx-background 100%%, -fx-background 100%%);",
                                                HIGHLIGHTED_CONTROL_INNER_BACKGROUND,
                                                DEFAULT_CONTROL_INNER_BACKGROUND,
                                                currentProgress
                                        )
                                );



                            } else {
                                setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                            }
                        }
                    }
                };
            }
        });

        ObservableList<String> items = FXCollections.observableArrayList (
                "Tom Clancy's Splinter Cell Chaos Theory",
                "Burnout 2: Point of Impact",
                "Gitaroo-Man",
                "Viewtiful Joe 2",
                "GrimGrimoire"
        );
        pcROMListView.setItems(items);
        VBox.setVgrow(pcROMListView, Priority.ALWAYS);

        vbox.getChildren().add(pcROMListView);

        return vbox;
    }

}
