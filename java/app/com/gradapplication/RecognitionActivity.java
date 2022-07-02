package app.com.gradapplication;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import app.com.gradapplication.ml.Model;



public class RecognitionActivity extends AppCompatActivity {


    Button gallery;
        ImageView imageView;
        int imageSize = 32;
        TextView result;

        Mat mRgba, hsvMat, hsvMatApplied, contourMat, lines;

        String letter;

        BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case BaseLoaderCallback.SUCCESS: {
                        break;
                    }
                    default: {
                        super.onManagerConnected(status);
                        break;
                    }
                }
            }
        };


        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_recognition);

            Intent i = getIntent();
            letter = i.getStringExtra("letter");

            Toast toast = Toast.makeText(this, letter , Toast.LENGTH_SHORT);
            toast.show();
            gallery = findViewById(R.id.button2);
            imageView = findViewById(R.id.imageView);
            result = findViewById(R.id.result);

            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 3);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }

        }

        @Override
        protected void onResume() {
            super.onResume();
            if (OpenCVLoader.initDebug()) {
                mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
            else {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallBack);
            }
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if(resultCode == RESULT_OK){
                if(requestCode == 3){
                    Bitmap image = (Bitmap) data.getExtras().get("data");
                    int dimension = Math.min(image.getWidth(), image.getHeight());
                    image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

                    imageView.setImageBitmap(seg(image));

                    image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                    classifyImage(image);
                }else{
                    Uri dat = data.getData();
                    Bitmap image = null;
                    try {
                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imageView.setImageBitmap(seg(image));

                    image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                    classifyImage(image);
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
        public void classifyImage(Bitmap image){
            try {
                Model model = Model.newInstance(getApplicationContext());

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 32, 32, 3}, DataType.FLOAT32);
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                byteBuffer.order(ByteOrder.nativeOrder());

                int[] intValues = new int[imageSize * imageSize];
                image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
                int pixel = 0;
                //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
                for(int i = 0; i < imageSize; i ++){
                    for(int j = 0; j < imageSize; j++){
                        int val = intValues[pixel++]; // RGB
                        byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                        byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                        byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                    }
                }

                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                Model.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                float[] confidences = outputFeature0.getFloatArray();
                // find the index of the class with the biggest confidence.
                int maxPos = 0;
                float maxConfidence = 0;
                for (int i = 0; i < confidences.length; i++) {
                    if (confidences[i] > maxConfidence) {
                        maxConfidence = confidences[i];
                        maxPos = i;
                    }
                }
                String[] classes =  {"A", "B", "C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};

                result.setText(classes[maxPos]);
                String s = "";
                double max  = 0;
                for(int i = 0; i < classes.length; i++){
                    // s += String.format("%s: %.1f%%", classes[i], confidences[i] * 100);
                    if(max < (confidences[i] * 100)){
                        max = confidences[i] * 100;
                        s = classes[i];
                    }
                }
                result.setText(s + " " + max);

                // Releases model resources if no longer used.
                model.close();
            } catch (IOException e) {
                // TODO Handle the exception
            }
        }

        Bitmap seg(Bitmap image){

            hsvMat = new Mat(imageSize, imageSize, CvType.CV_8UC4);
            mRgba = new Mat(imageSize, imageSize, CvType.CV_8UC4);
            hsvMatApplied = new Mat(imageSize, imageSize, CvType.CV_8UC4);

            mRgba = new Mat();
            Utils.bitmapToMat(image, mRgba);

            Mat mRgbaT = mRgba.t();
            Core.flip(mRgba.t(), mRgbaT, 1);
            Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());

//        Size sz = new Size(394,700);
//        Imgproc.resize(mRgba, mRgba, sz);
            Imgproc.blur(mRgba, mRgba, new Size(7, 7));
            Imgproc.cvtColor(mRgba, hsvMat, Imgproc.COLOR_BGR2HSV);

            Scalar hsvMin = new  Scalar(0, 0, 0);
            Scalar hsvMax = new Scalar(180, 132, 132);

            Core.inRange(hsvMat, hsvMin, hsvMax, hsvMatApplied);

            Bitmap bmp = null;
            bmp = Bitmap.createBitmap(hsvMatApplied.cols(), hsvMatApplied.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(hsvMatApplied, bmp);
            return bmp;
        }
    }
