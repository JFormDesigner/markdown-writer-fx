package org.markdownwriterfx.editor;

import java.io.File;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.DirectoryChooserBuilder;
import javafx.stage.Stage;

public class JavaFxTest extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        final Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
			public void handle(ActionEvent event) {
                DirectoryChooserBuilder builder = DirectoryChooserBuilder.create();
                builder.title("Hello World");
                String cwd = System.getProperty("user.dir");
                File file = new File(cwd);
                builder.initialDirectory(file);
                DirectoryChooser chooser = builder.build();
                File chosenDir = chooser.showDialog(primaryStage);
                if (chosenDir != null) {
                  System.out.println(chosenDir.getAbsolutePath());
                } else {
                 System.out.print("no directory chosen");
                }
            }
        });

        StackPane root = new StackPane();
        root.getChildren().add(btn);

        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
    }
}