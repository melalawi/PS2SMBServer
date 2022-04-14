package org.ps2.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.ps2.Main;
import org.ps2.rom.ROM;
import org.ps2.rom.RomManager;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;


public class ClientApplication extends Application {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 540;

    private static final String DEFAULT_CONTROL_INNER_BACKGROUND = "rgb(40, 40, 40)";
    private static final String HIGHLIGHTED_CONTROL_INNER_BACKGROUND = "rgb(20, 20, 20)";

    private static final String QUESTION = "?";
    private static final String CHECK = "✔";
    private static final String ERROR = "❌";

    private static Timer refreshUITimer;

    private static ROM selectedRom;

    private static Stage stage;
    private static ListView<ROM> romListView;
    private static FilteredList<ROM> filteredROMs;
    private static TextField romSearchField;
    private static Button startPs2ServerButton;
    private static Text statusLabelText;
    private static Text subtitleText;

    private static TextFlow loggingArea;

    private static RomManager romManager;
    private static ClientLogger clientLogger;

    public static void run(final ClientLogger logger, final RomManager manager) {
        refreshUITimer = new Timer();
        clientLogger = logger;
        romManager = manager;

        launch(new String[0]);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        loggingArea = new TextFlow();
        clientLogger.setLoggingArea(loggingArea);

        romListView = new ListView<>();

        primaryStage.setTitle("PS2 Game Loader");

        Pane root = new Pane();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add("/theme.css");
        primaryStage.setMinHeight(HEIGHT);
        primaryStage.setMinWidth(WIDTH);

        BorderPane borderPane = new BorderPane();

        borderPane.setTop(generateToolbar());
        borderPane.setLeft(generateLeftSection());
        borderPane.setRight(generateRightSection());
        borderPane.setCenter(generateMiddleSection());

        borderPane.prefHeightProperty().bind(scene.heightProperty());
        borderPane.prefWidthProperty().bind(scene.widthProperty());

        root.getChildren().addAll(borderPane);
        primaryStage.setScene(scene);

        primaryStage.getIcons().add(new Image(getClass().getResource("/rat.png").toString()));

        primaryStage.show();

        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.F1) {
                    showAboutWindow();
                }
            }
        });

        hookUps();
    }

    @Override
    public void stop() {
        refreshUITimer.cancel();
        Main.close();
    }

    public static void refresh(final boolean immediate) {
        if (immediate) {
            updateView();
        } else {
            Platform.runLater(() -> {
                updateView();
            });
        }
    }

    private void hookUps() {
        refreshUITimer.schedule(new TimerTask() {
            @Override
            public void run() {
                refresh(false);
            }
        }, 0, 1000);

        romListView.setOnMouseClicked(click -> {
            ROM selected = romListView.getSelectionModel().getSelectedItem();

            if (selected == null) {
                return;
            }
            selectedRom = selected;
            refreshSelectedROM();

            if (click.getClickCount() == 2 && selectedRom.getLoadProgress() == 0) {
                try {
                    romManager.unpack(selected);
                } catch (RejectedExecutionException e) {
                    Alert alert = new Alert(Alert.AlertType.NONE, "Please wait for current jobs to complete! 😾");

                    alert.setTitle("Too many unzips!");
                    alert.initOwner(stage.getScene().getWindow());
                    alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
                    alert.show();
                }
            }
        });

        startPs2ServerButton.setOnAction(e -> {
            startPs2ServerButton.setDisable(true);
            Main.startPS2Server();
        });

        romSearchField.textProperty().addListener(((observable, oldValue, newValue) -> {
            romListView.getSelectionModel().clearSelection();
            filteredROMs.setPredicate(ClientApplication::romFilterPredicate);
        }));
    }

    private static boolean romFilterPredicate(final ROM rom) {
        final String filter = romSearchField.getText();
        if (filter == null || filter.isEmpty()) {
            return true;
        }

        String lowerCaseSearch = filter.toLowerCase();
        return rom.getCleanName().toLowerCase().contains(lowerCaseSearch);
    }

    private void refreshSelectedROM() {
        if (selectedRom == null) {
            return;
        }

        Map<String, List<File>> artFiles = romManager.getRomArtFiles(selectedRom);
        Random random = new Random();

        if (!artFiles.isEmpty()) {
            ImageView title = (ImageView) stage.getScene().lookup("#ROMTitle");
            ImageView screenshot = (ImageView) stage.getScene().lookup("#ROMScreenshot");
            ImageView boxBack = (ImageView) stage.getScene().lookup("#ROMBoxBack");
            ImageView boxCenter = (ImageView) stage.getScene().lookup("#ROMBoxCenter");
            ImageView boxFront = (ImageView) stage.getScene().lookup("#ROMBoxFront");

            File file = null;
            for (File candidateTitle : artFiles.get("TITLE")) {
                if (candidateTitle.exists()) {
                    file = candidateTitle;
                    break;
                }
            }
            if (file != null) {
                title.setImage(new Image(file.toString()));
            } else {
                title.setImage(null);
            }


            for (File candidateTitle : artFiles.get("SCREENSHOT")) {
                if (candidateTitle.exists()) {
                    file = candidateTitle;
                    break;
                }
            }
            if (file != null) {
                screenshot.setImage(new Image(file.toString()));
            } else {
                screenshot.setImage(null);
            }


            for (File candidateTitle : artFiles.get("BACK")) {
                if (candidateTitle.exists()) {
                    file = candidateTitle;
                    break;
                }
            }
            if (file != null) {
                boxBack.setImage(new Image(file.toString()));
            } else {
                boxBack.setImage(null);
            }

            for (File candidateTitle : artFiles.get("SIDE")) {
                if (candidateTitle.exists()) {
                    file = candidateTitle;
                    break;
                }
            }
            if (file != null) {
                boxCenter.setImage(new Image(file.toString()));
            } else {
                boxCenter.setImage(null);
            }

            for (File candidateTitle : artFiles.get("FRONT")) {
                if (candidateTitle.exists()) {
                    file = candidateTitle;
                    break;
                }
            }
            if (file != null) {
                boxFront.setImage(new Image(file.toString()));
            } else {
                boxFront.setImage(null);
            }

        }

    }

    private static void updateView() {
        ObservableList<ROM> allROMs = FXCollections.observableArrayList(romManager.getROMList());
        List<ROM> activeUnzips = romManager.getCurrentUnzipJobs();

        if (activeUnzips.isEmpty()) {
            if (romManager.isLoading()) {
                statusLabelText.setText("Building ROM list...");
            } else {
                statusLabelText.setText("Ready.");
            }
        } else if (activeUnzips.size() == 1) {
            statusLabelText.setText("Unzipping " + activeUnzips.get(0).getCleanName() + "...");
        } else {
            statusLabelText.setText(String.format("Unzipping %d ROMs...", activeUnzips.size()));
        }

        // Search during load
        filteredROMs = new FilteredList<ROM>(allROMs, data -> true);
        filteredROMs.setPredicate(ClientApplication::romFilterPredicate);

        romListView.setItems(filteredROMs);

        subtitleText.setText(String.format("(%d ROMs)", allROMs.size()));

        romListView.refresh();
    }

    private Node generateToolbar() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        hbox.setStyle("-fx-background-color: -fx-control-inner-background-alt;");

        hbox.setAlignment(Pos.BASELINE_RIGHT);

        startPs2ServerButton = new Button("Start PS Server");
        startPs2ServerButton.setPrefSize(100, 20);


        hbox.getChildren().addAll(startPs2ServerButton);

        return hbox;
    }

    public Node generateMiddleSection() {
        BorderPane container = new BorderPane();
        BorderPane titleBox = new BorderPane();
        GridPane screenshotBox = new GridPane();
        HBox gameBox = new HBox();


        ImageView title = new ImageView();
        ImageView screenshot = new ImageView();
        ImageView boxBack = new ImageView();
        ImageView boxCenter = new ImageView();
        ImageView boxFront = new ImageView();

        title.setFitHeight(100);
        boxBack.setFitHeight(100);
        boxCenter.setFitHeight(100);
        boxFront.setFitHeight(100);
        titleBox.setPrefHeight(100);
        titleBox.setPadding(new Insets(20, 10, 20, 10));

        screenshotBox.setMinSize(350, 350);
        screenshotBox.setPrefSize(350, 350);
        screenshotBox.setAlignment(Pos.CENTER);
        screenshotBox.setPadding(new Insets(10, 10, 10, 10));
        screenshot.fitHeightProperty().bind(screenshotBox.heightProperty());
        screenshot.fitWidthProperty().bind(screenshotBox.widthProperty());

        title.setPreserveRatio(true);
        screenshot.setPreserveRatio(true);
        boxBack.setPreserveRatio(true);
        boxCenter.setPreserveRatio(true);
        boxFront.setPreserveRatio(true);

        screenshot.setSmooth(false);

        title.setId("ROMTitle");
        screenshot.setId("ROMScreenshot");
        boxBack.setId("ROMBoxBack");
        boxCenter.setId("ROMBoxCenter");
        boxFront.setId("ROMBoxFront");

        screenshotBox.getChildren().add(screenshot);

        gameBox.getChildren().addAll(
                boxBack,
                boxCenter,
                boxFront
        );

        titleBox.setLeft(title);
        titleBox.setRight(gameBox);

        container.setTop(titleBox);
        container.setCenter(screenshotBox);

        return container;
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

        ScrollPane scrollPane = new ScrollPane();
        loggingArea.setDisable(true);
        loggingArea.getChildren().addListener(
                (ListChangeListener<Node>) ((change) -> {
                    loggingArea.layout();
                    scrollPane.layout();
                    scrollPane.setVvalue(1.0f);
                }));
        loggingArea.setPrefWidth(300);
        loggingArea.setMaxWidth(300);
        scrollPane.setPrefWidth(300);
        scrollPane.setContent(loggingArea);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        vbox.getChildren().add(scrollPane);

        return vbox;
    }

    public Node generateLeftSection() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        HBox titleGroup = new HBox();
        titleGroup.setAlignment(Pos.BOTTOM_LEFT);
        Text title = new Text("ROMs ");
        subtitleText = new Text("");
        statusLabelText = new Text("");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        subtitleText.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        statusLabelText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        subtitleText.setFill(Color.GRAY);
        statusLabelText.setFill(Color.GRAY);

        titleGroup.getChildren().add(title);
        titleGroup.getChildren().add(subtitleText);
        vbox.getChildren().add(titleGroup);

        romListView.setCellFactory(new Callback<ListView<ROM>, ListCell<ROM>>() {
            @Override
            public ListCell<ROM> call(ListView<ROM> param) {
                return new ListCell<ROM>() {
                    @Override
                    protected void updateItem(ROM rom, boolean empty) {
                        super.updateItem(rom, empty);

                        if (rom == null || rom == null || empty) {
                            setText(null);
                            setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                        } else {
                            setText(rom.getCleanName());

                            if (rom.getLoadProgress() == 100) {
                                setStyle("-fx-control-inner-background: " + HIGHLIGHTED_CONTROL_INNER_BACKGROUND + ";");
                            } else if (rom.getLoadProgress() == 0) {
                                setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                            } else {
                                int currentProgress = rom.getLoadProgress();

                                setText(currentProgress + "% - " + rom.getCleanName());

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
                            }
                        }
                    }
                };
            }
        });
        romSearchField = new TextField();
        romSearchField.setPromptText("Search games...");
        VBox.setVgrow(romListView, Priority.ALWAYS);

        vbox.getChildren().addAll(romSearchField, romListView, statusLabelText);

        return vbox;
    }

    private void showAboutWindow() {
        final Stage dialog = new Stage();

        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);

        BorderPane borderPane = new BorderPane();

        Text title = new Text("About PS2SMBServer");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        HBox titleBox = new HBox();
        titleBox.setPrefHeight(100);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(20, 10, 20, 10));
        titleBox.getChildren().add(title);

        Label rant = new Label("about");
        rant.setWrapText(true);
        rant.setStyle("-fx-font-family: \"Comic Sans MS\"; -fx-font-size: 20; -fx-text-fill: white;");
        HBox rantBox = new HBox();
        rantBox.setPrefHeight(100);
        rantBox.setAlignment(Pos.CENTER);
        rantBox.setPadding(new Insets(20, 10, 20, 10));
        rantBox.setMaxWidth(400);
        rantBox.getChildren().add(rant);

        HBox leezardBox = new HBox();
        MediaView leezard = leezard();
        leezard.setFitHeight(300);
        leezard.setFitWidth(300);
        leezardBox.setAlignment(Pos.CENTER);
        leezardBox.setPadding(new Insets(20, 10, 20, 10));

        MediaPlayer mix = new MediaPlayer(new Media(getClass().getResource("/mix.mp3").toString()));
        mix.setOnEndOfMedia(new Runnable() {
            public void run() {
                mix.seek(Duration.ZERO);
            }
        });
        leezardBox.getChildren().add(leezard);
        leezardBox.getChildren().add(new MediaView(mix));

        borderPane.setTop(titleBox);
        borderPane.setLeft(leezardBox);
        borderPane.setRight(rantBox);

        Scene dialogScene = new Scene(borderPane, 712, 512);
        dialogScene.getStylesheets().add("/theme.css");
        dialog.setScene(dialogScene);

        dialog.setMinHeight(512);
        dialog.setMinWidth(712);
        dialog.setMaxHeight(512);
        dialog.setMaxWidth(712);
        dialog.setResizable(false);
        mix.play();
        leezard.getMediaPlayer().play();
        dialog.showAndWait();

        mix.stop();

    }
}
