 /**
 * @providesModule RNSecKey
 * @flow
 */
import ReactNative from 'react-native';

const { NativeModules } = ReactNative;
const NativeRNSecKey = NativeModules.RNSecKey;

/**
 * High-level docs for the RNSecKey iOS API can be written here.
 */

var RNSecKey = {
  test: function() {
    NativeRNSecKey.test();
  },
  generateKey: (tag, callback) => NativeRNSecKey.generateKey(tag, callback),
  getPublicKey: (tag, callback) => NativeRNSecKey.getPublicKey(tag, callback),
  getSignature: (tag, nonce, message, callback) => {
    if(typeof message === 'function'){
      callback = message;
      message = null;
    }
    NativeRNSecKey.getSignature(tag, nonce, message, callback);
  },  removeKeyPair: (callback) => NativeRNSecKey.removeKeyPair(callback),
  isFingerprintSupported: (callback) => NativeRNSecKey.isFingerprintSupported(callback),
  isLockScreenEnabled: (callback) => NativeRNSecKey.isLockScreenEnabled(callback),
  isEligibleForFingerprint: (callback) => NativeRNSecKey.isEligibleForFingerprint(callback),
  getDeviceName: (callback) => NativeRNSecKey.getDeviceName(callback),
  getDeviceVersion: (callback) => NativeRNSecKey.getDeviceVersion(callback),
  getDeviceId: (callback) => NativeRNSecKey.getDeviceId(callback),
  saveDeviceId: (deviceId, callback) => NativeRNSecKey.saveDeviceId(deviceId, callback),
  removeDeviceId: (callback) => NativeRNSecKey.removeDeviceId(callback),
  isRooted: (callback) => NativeRNSecKey.isRooted(callback),

};

export default RNSecKey;
