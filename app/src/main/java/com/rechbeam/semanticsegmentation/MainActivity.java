package com.rechbeam.semanticsegmentation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {
    private Classifier detector;

//    private static final int TF_OD_API_INPUT_SIZE = 300;

        private static final int TF_OD_API_INPUT_SIZE = 257;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
//    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";

        private static final String TF_OD_API_MODEL_FILE = "deeplabv3_257_mv_gpu.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            false);
//                            TF_OD_API_IS_QUANTIZED);
            Log.i("debug", "created detector");
        } catch (final IOException e) {

            Log.i("debug", "didn't created detector");
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    public void sendMessage(View view) {
        //Create an Intent with action as ACTION_PICK
        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        // Launching the Intent
        startActivityForResult(intent,1111);
    }

    public void onActivityResult(int requestCode,int resultCode,Intent data){
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case 1111:
                    //data.getData returns the content URI for the selected Image
                    final Uri imageUri = data.getData();
                    final InputStream imageStream;
                    try {
                        imageStream = getContentResolver().openInputStream(imageUri);
                    }catch(Exception e){
                        return;
                    }
                    final Bitmap originalImage = BitmapFactory.decodeStream(imageStream);
                    Bitmap background = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888);

                    float originalWidth = originalImage.getWidth();
                    float originalHeight = originalImage.getHeight();

                    Canvas canvas = new Canvas(background);

                    float scale = TF_OD_API_INPUT_SIZE / originalWidth;

                    float xTranslation = 0.0f;
                    float yTranslation = (TF_OD_API_INPUT_SIZE - originalHeight * scale) / 2.0f;

                    Matrix transformation = new Matrix();
                    transformation.postTranslate(xTranslation, yTranslation);
                    transformation.preScale(scale, scale);

                    Paint paint = new Paint();
                    paint.setFilterBitmap(true);

                    canvas.drawBitmap(originalImage, transformation, paint);

                    final List<Classifier.Recognition> results = detector.recognizeImage(background);
                    final Bitmap copiedImage = Bitmap.createBitmap(background);
                    final Canvas canvas2 = new Canvas(copiedImage);
                    final Paint paint2 = new Paint();

                    paint2.setColor(Color.RED);
                    paint2.setAlpha(125);
                    paint2.setStyle(Paint.Style.FILL);
//                    paint2.setStyle(Paint.Style.STROKE);
//                    paint2.setStrokeWidth(2.0f);
                    for (final Classifier.Recognition result : results) {
//                        Log.i("label", ""+result.getTitle());
//
//                        if (!result.getTitle().equals("person"))
//                            continue;
//                        Log.i("getConfidence", ""+result.getConfidence());
//                        final RectF location = result.getLocation();
                        final float[][][][] location = result.getLocation();
//
//                        Log.i("location", Arrays.deepToString(location));
////                        person = 15 index
//                        float min = 10000;
                        for(int i=0;i<location[0].length;++i){
                            for(int j=0;j<location[0][0].length;++j){
                                float max = -10000;
                                int argmax = -1;
                                for(int k=0;k<location[0][0][0].length;++k){
                                    if (location[0][i][j][k] > max){
                                        max = location[0][i][j][k];
                                        argmax = k;
                                    }
                                }
                                if (argmax == 15)
                                    canvas2.drawPoint(j,i, paint2);
                            }
                        }
//                        Log.i("min", ""+min);
//
//                        Log.i("max", ""+max);
//                        if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
//                            canvas2.drawRect(location, paint2);
//
////                            cropToFrameTransform.mapRect(location);
////
////                            result.setLocation(location);
////                            mappedRecognitions.add(result);
//                        }
                    }

                    ImageView iv = findViewById(R.id.imageView);
                    iv.setImageBitmap(copiedImage);
//
//                    iv.setImageURI(selectedImage);
                    break;
            }
    }
}
