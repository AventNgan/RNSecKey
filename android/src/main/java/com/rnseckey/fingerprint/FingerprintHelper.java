package com.rnseckey.fingerprint;

import android.app.Activity;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.BoolRes;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import com.rnseckey.R;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.concurrent.Callable;

import static android.R.attr.key;
import static android.R.attr.numbersBackgroundColor;

/**
 * Created by woonchee.tang on 16/01/2017.
 */

public class FingerprintHelper {
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";

    KeyguardManager mKeyguardManager;
    FingerprintManager mFingerprintManager;
    FingerprintAuthenticationDialogFragment mFragment;
    KeyStore mKeyStore;
    KeyPairGenerator mKeyPairGenerator;
    Signature mSignature;
    SharedPreferences mSharedPreferences;
    public static final String KEY_NAME = "my_key";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public FingerprintHelper(Context context){
        FingerprintModule m = new FingerprintModule(context);
        mKeyguardManager = m.providesKeyguardManager(context);
        mFingerprintManager = m.providesFingerprintManager(context);
        mSignature = m.providesSignature(m.providesKeystore());
        mKeyStore = m.providesKeystore();
        mSignature = m.providesSignature(mKeyStore);
        mSharedPreferences = m.providesSharedPreferences(context);
        mKeyPairGenerator = m.providesKeyPairGenerator();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean hasEnrolledFingerprints(){
       return mFingerprintManager.hasEnrolledFingerprints();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean hasFingerprintSupport(){
        return mFingerprintManager.isHardwareDetected();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public PublicKey createKeyPair() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.

        try {
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            PublicKey publicKey = getExistingKey();

            if(publicKey!=null){
                return publicKey;
            }
            if(mFingerprintManager.isHardwareDetected()){
                mKeyPairGenerator.initialize(
                        new KeyGenParameterSpec.Builder(KEY_NAME,
                                KeyProperties.PURPOSE_SIGN)
                                .setDigests(KeyProperties.DIGEST_SHA256)
                                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                                // Require the user to authenticate with a fingerprint to authorize
                                // every use of the private key
                                .setUserAuthenticationRequired(true)
                                .build());
            }else{
                mKeyPairGenerator.initialize(
                        new KeyGenParameterSpec.Builder(KEY_NAME,
                                KeyProperties.PURPOSE_SIGN)
                                .setDigests(KeyProperties.DIGEST_SHA256)
                                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                                // Require the user to authenticate with a fingerprint to authorize
                                // every use of the private key
                                .setUserAuthenticationRequired(false)
                                .build());
            }
            KeyPair key = mKeyPairGenerator.generateKeyPair();

            return key.getPublic();
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

    }
    public void clearKey(){
        try {
            mKeyStore.load(null);
            mKeyStore.deleteEntry(KEY_NAME);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private PublicKey getExistingKey(){
        try {
            mKeyStore.load(null);
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) mKeyStore.getEntry(KEY_NAME, null);
            return entry.getCertificate().getPublicKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void authenticate(String nonce, Activity context, final FingerprintAuthenticationDialogFragment.FingerprintListener callable, String scode){
        mFragment = (FingerprintAuthenticationDialogFragment)Fragment.instantiate(context,FingerprintAuthenticationDialogFragment.class.getName());
        try {

            boolean status = initSignature();
            if ((status && hasFingerprintSupport() && hasEnrolledFingerprints()) || (status && !hasFingerprintSupport())) {
                // Show the fingerprint dialog. The user has the option to use the fingerprint with
                // crypto, or you can fall back to using a server-side verified password.
//                if(hasEnrolledFingerprints())
                mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mSignature));
                mFragment.setSuccessRunable(callable);
                mFragment.setNonce(nonce);
                boolean useFingerprintPreference = mSharedPreferences
                        .getBoolean(context.getString(R.string.use_fingerprint_to_authenticate_key),
                                true);
                if (useFingerprintPreference) {
                    mFragment.setStage(
                            FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
                } else {
                    mFragment.setStage(
                            FingerprintAuthenticationDialogFragment.Stage.PASSWORD);
                }

                if(scode!=null){
                    mFragment.setSecretCode(scode);
                }

                mFragment.show(context.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                // This happens if the lock screen has been disabled or or a fingerprint got
                // enrolled. Thus show the dialog to authenticate with their password first
                // and ask the user if they want to authenticate with fingerprints in the
                // future
                mFragment.setSuccessRunable(callable);
                mFragment.setStage(
                        FingerprintAuthenticationDialogFragment.Stage.NEW_FINGERPRINT_ENROLLED);
                mFragment.show(context.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }

        }catch (UserNotAuthenticatedException e){
            mFragment.setSuccessRunable(callable);
            mFragment.setNonce(nonce);
            mFragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
            mFragment.show(context.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            mFragment.setOnAuthenticatedListener(new FingerprintAuthenticationDialogFragment.OnAuthenticatedListener() {
                @Override
                public void onAuthenticated() {
                    try{
                        initSignature();
                        mFragment.setCryptoObject(new FingerprintManager.CryptoObject(mSignature));
                    }catch (Exception e){
                        e.printStackTrace();
                        callable.onFail(-11);
                    }
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean initSignature() throws UserNotAuthenticatedException {
        try {
            mKeyStore.load(null);
            PrivateKey key = (PrivateKey) mKeyStore.getKey(KEY_NAME, null);
            mSignature.initSign(key);
            return true;
        }catch(UserNotAuthenticatedException e){
            throw e;
        }
        catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }
}