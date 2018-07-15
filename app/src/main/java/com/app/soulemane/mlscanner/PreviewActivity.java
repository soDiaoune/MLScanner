package com.app.soulemane.mlscanner;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudDocumentTextDetector;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.otaliastudios.cameraview.CameraUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PreviewActivity extends AppCompatActivity {


    @BindView(R.id.image_view)
    ImageView image;

    @BindView(R.id.button_text)
    Button button;

    @BindView(R.id.button_cloud_text)
    Button cloudButton;

    @BindView(R.id.graphic_overlay)
    GraphicOverlay graphicOverlay;



    private byte[] capture;
    private static WeakReference<byte[]> imageByte;

    public static void setImage(@Nullable byte[] im) {
        imageByte = im != null ? new WeakReference<>(im) : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        ButterKnife.bind(this);
        Intent mIntent = getIntent();
        capture = imageByte == null ? null : imageByte.get();
        if (capture != null){
            CameraUtils.decodeBitmap(capture, 1000, 1000, new CameraUtils.BitmapCallback() {
                @Override
                public void onBitmapReady(final Bitmap bitmap) {
                    image.setImageBitmap(bitmap);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            runTextRecognition(bitmap);
                        }
                    });
                    cloudButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            runCloudTextRecognition(bitmap);
                        }
                    });
                }
            });
        }



    }

    private void runTextRecognition(Bitmap content) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(content);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();
        button.setEnabled(false);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                button.setEnabled(true);
                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                button.setEnabled(true);
                                e.printStackTrace();
                            }
                        });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        List<FirebaseVisionText.Block> blocks = texts.getBlocks();
        if (blocks.size() == 0) {
            showToast("No text found");
            return;
        }
        graphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(graphicOverlay, elements.get(k));
                    graphicOverlay.add(textGraphic);

                }
            }
        }
    }

    private void runCloudTextRecognition(Bitmap content) {
        FirebaseVisionCloudDetectorOptions options =
                new FirebaseVisionCloudDetectorOptions.Builder()
                        .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                        .setMaxResults(15)
                        .build();
        cloudButton.setEnabled(false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(content);
        FirebaseVisionCloudDocumentTextDetector detector = FirebaseVision.getInstance()
                .getVisionCloudDocumentTextDetector(options);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionCloudText>() {
                            @Override
                            public void onSuccess(FirebaseVisionCloudText texts) {
                                cloudButton.setEnabled(true);
                                processCloudTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                cloudButton.setEnabled(true);
                                e.printStackTrace();
                            }
                        });
    }

    private void processCloudTextRecognitionResult(FirebaseVisionCloudText text) {
        // Task completed successfully
        if (text == null) {
            showToast("No text found");
            return;
        }
        graphicOverlay.clear();
        List<FirebaseVisionCloudText.Page> pages = text.getPages();
        for (int i = 0; i < pages.size(); i++) {
            FirebaseVisionCloudText.Page page = pages.get(i);
            List<FirebaseVisionCloudText.Block> blocks = page.getBlocks();
            for (int j = 0; j < blocks.size(); j++) {
                List<FirebaseVisionCloudText.Paragraph> paragraphs = blocks.get(j).getParagraphs();
                for (int k = 0; k < paragraphs.size(); k++) {
                    FirebaseVisionCloudText.Paragraph paragraph = paragraphs.get(k);
                    List<FirebaseVisionCloudText.Word> words = paragraph.getWords();
                    for (int l = 0; l < words.size(); l++) {
                        GraphicOverlay.Graphic cloudTextGraphic = new CloudTextGraphic(graphicOverlay, words
                                .get(l));
                        graphicOverlay.add(cloudTextGraphic);
                    }
                }
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
