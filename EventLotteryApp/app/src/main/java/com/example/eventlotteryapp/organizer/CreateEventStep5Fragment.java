package com.example.eventlotteryapp.organizer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.eventlotteryapp.databinding.FragmentCreateEventStep5Binding;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class CreateEventStep5Fragment extends Fragment {

    private FragmentCreateEventStep5Binding binding;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private CreateEventViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventStep5Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(CreateEventViewModel.class);

        // Set up toolbar back icon
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof CreateEventActivity) {
                ((CreateEventActivity) getActivity()).handleBackPress();
            }
        });

        // Register for activity result
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    binding.posterImageView.setImageURI(uri);
                    // Store the image URI in ViewModel
                    viewModel.posterImageUri.setValue(uri);
                    // Convert to base64 and store
                    convertImageToBase64(uri);
                }
            }
        );

        binding.uploadImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });
        
        // Load existing image if available
        Uri existingUri = viewModel.posterImageUri.getValue();
        if (existingUri != null) {
            binding.posterImageView.setImageURI(existingUri);
        }
    }
    
    private void convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                // Compress and convert to base64
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                String base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                
                // Store in ViewModel with data URI prefix
                String dataUri = "data:image/webp;base64," + base64String;
                viewModel.posterImageBase64.setValue(dataUri);
            }
        } catch (Exception e) {
            android.util.Log.e("CreateEventStep5Fragment", "Error converting image to base64", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

