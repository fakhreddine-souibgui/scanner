package com.example.scanner

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.view.Menu
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var inputImageBtn:MaterialButton
    private lateinit var recognizeTextBtn:MaterialButton
    private lateinit var imageIv:ImageView
    private lateinit var recognizedTextEt:EditText

    private companion object{
        private const val CAMERA_REQUEST_CODE =100
        private const val STORAGE_REQUEST_CODE=101
    }

    private var imageUri:Uri?= null


    private lateinit var cameraPermissions:Array<String>
    private lateinit var storagePermissions:Array<String>
    private lateinit var progressDialog: ProgressDialog
    private lateinit var textRecognizer:TextRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


       //init UI view
        inputImageBtn = findViewById(R.id.inputImageBtn)
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn)
        imageIv = findViewById(R.id.imageIv)
        recognizedTextEt = findViewById(R.id.recognizedTextEt)
       //**************
        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)



        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("please wait ")
        progressDialog.setCanceledOnTouchOutside(false)


        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)




        //handle click ,show input image dialog
        inputImageBtn.setOnClickListener{
            showInputImageDialog()

        }

        recognizeTextBtn.setOnClickListener {

            if (imageUri == null){
                showToast("Pick Image First ...")
            }
            else{

              recognizeTextFromImage()
            }
        }
    }

    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Preparing Image ...")
        progressDialog.show()
        try {
            val inputImage = InputImage.fromFilePath(this,imageUri!!)
            progressDialog.setMessage("Recognizing text ...")

            val textTaskResult =textRecognizer.process(inputImage)
                .addOnSuccessListener {text ->
                    progressDialog.dismiss()
                    val recognizedText = text.text
                    recognizedTextEt.setText(recognizedText)

                }
                .addOnFailureListener { e ->

                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to ${e.message}")

                }

        }
        catch (e:Exception){
            progressDialog.dismiss()
            showToast("Failed to prepare image due to ${e.message}")
        }
    }

    private fun showInputImageDialog() {

        val popupMenu = PopupMenu(this , inputImageBtn)

        popupMenu.menu.add(Menu.NONE, 1, 1, "CAMERA")
        popupMenu.menu.add(Menu.NONE, 2, 2, "GALLERY")


       popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val id  = menuItem.itemId
            if(id == 1){
                if (checkCameraPermission()){
                    pickImageCamera()

                }
                else {
                    requestCameraPermissions()

                }

            }
            else if (id == 2) {

                if (checkStoragePermission()) {

                    pickImageGallery()
                }
                else{
                    requestStoragePermissions()

                }
            }
            return@setOnMenuItemClickListener true
        }

    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLancher.launch(intent)
    }

    private val galleryActivityResultLancher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if (result.resultCode== Activity.RESULT_OK){
               val data = result.data
                imageUri = data!!.data

                imageIv.setImageURI(imageUri)

            }
            else{

              showToast("Cancelled...!")
            }
        }
    private fun pickImageCamera(){
val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE,"Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION,"Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)

    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if (result.resultCode == Activity.RESULT_OK){

                imageIv.setImageURI(imageUri)
            }
            else{
                showToast("Cancelled ....")
            }
        }
    private fun checkStoragePermission(): Boolean{
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermission(): Boolean{
        val cameraResult = ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED



        return cameraResult && storageResult
    }

    private fun requestStoragePermissions(){

        ActivityCompat.requestPermissions(this ,storagePermissions, STORAGE_REQUEST_CODE)

    }
    private fun requestCameraPermissions(){

        ActivityCompat.requestPermissions(this,cameraPermissions, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {

                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                 if (cameraAccepted && storageAccepted){

                     pickImageCamera()
                 }
                    else{
                        showToast("Camera & storage permission are required ...")

                    }

                }
            }

            STORAGE_REQUEST_CODE -> {
                if(grantResults.isNotEmpty()){

                    val storageAccepted = grantResults[0] ==PackageManager.PERMISSION_GRANTED
                    if(storageAccepted){

                        pickImageGallery()

                    }
                    else{

                        showToast("Storage permission is required ...")

                    }

                }

            }

        }
    }




private fun showToast(message: String){
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()


}



}