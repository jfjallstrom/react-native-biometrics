package com.rnbiometrics;

import android.annotation.TargetApi;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;
import com.rnbiometrics.R;

/**
 * Created by brandon on 4/5/18.
 */

@TargetApi(Build.VERSION_CODES.M)
public class ReactNativeBiometricsHelper extends FingerprintManager.AuthenticationCallback {

    private static final long ERROR_TIMEOUT_MILLIS = 1600;
    private static final long SUCCESS_DELAY_MILLIS = 1300;

    private final FingerprintManager fingerprintManager;
    private final String recognized;
    private final String notRecognized;
    private final String hint;
    private final ImageView icon;
    private final TextView errorTextView;
    private final ReactNativeBiometricsCallback callback;
    private CancellationSignal cancellationSignal;
    private Runnable resetErrorTextRunnable;

    private boolean selfCancelled;

    ReactNativeBiometricsHelper(FingerprintManager fingerprintManager, ImageView icon,
                                  TextView errorTextView, String recognized, String notRecognized, String hint, ReactNativeBiometricsCallback callback) {
        this.fingerprintManager = fingerprintManager;
        this.recognized = recognized;
        this.notRecognized = notRecognized;
        this.hint = hint;
        this.icon = icon;
        this.errorTextView = errorTextView;
        this.callback = callback;
    }

    public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        selfCancelled = false;

        cancellationSignal = new CancellationSignal();
        fingerprintManager
                .authenticate(cryptoObject, cancellationSignal, 0 /* flags */, this, null);
        icon.setImageResource(R.drawable.ic_fp_40px);
    }

    public void stopListening() {
        if (cancellationSignal != null) {
            selfCancelled = true;
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        if (!selfCancelled) {
            showError(errString);
            icon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onError();
                }
            }, ERROR_TIMEOUT_MILLIS);
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        showError(helpString);
    }

    @Override
    public void onAuthenticationFailed() {
        showError(this.notRecognized);
    }

    @Override
    public void onAuthenticationSucceeded(final FingerprintManager.AuthenticationResult result) {
        errorTextView.removeCallbacks(this.resetErrorTextRunnable);
        icon.setImageResource(R.drawable.ic_fingerprint_success);
        errorTextView.setTextColor(errorTextView.getResources().getColor(R.color.success_color, null));
        errorTextView.setText(this.recognized);
        icon.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onAuthenticated(result.getCryptoObject());
            }
        }, SUCCESS_DELAY_MILLIS);
    }

    private void showError(CharSequence error) {
        icon.setImageResource(R.drawable.ic_fingerprint_error);
        errorTextView.setText(error);
        errorTextView.setTextColor(errorTextView.getResources().getColor(R.color.warning_color, null));
        this.resetErrorTextRunnable = createResetErrorTextRunnable(this.hint);
        errorTextView.removeCallbacks(this.resetErrorTextRunnable);
        errorTextView.postDelayed(this.resetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
    }

    private Runnable createResetErrorTextRunnable(final String hint) {
        Runnable resetErrorTextRunnable = new Runnable() {
            public void run(){
                errorTextView.setTextColor(
                        errorTextView.getResources().getColor(R.color.hint_color, null));
                errorTextView.setText(hint);
                icon.setImageResource(R.drawable.ic_fp_40px);
            }
        };
        return resetErrorTextRunnable;
    }
}
