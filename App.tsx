import React, {useEffect, useState} from 'react';
import {View, Button, Text} from 'react-native';
import {NativeEventEmitter, NativeModules} from 'react-native';

const {MyFingerprintModule} = NativeModules;
const eventEmitter = new NativeEventEmitter(MyFingerprintModule);

const App = () => {
  const [fingerprintData, setFingerprintData] = useState('');
  const [fingerprintError, setFingerprintError] = useState('');

  useEffect(() => {
    const fingerprintCaptureListener = eventEmitter.addListener(
      'FingerprintCaptured',
      fingerprintImageData => {
        setFingerprintData(fingerprintImageData);
        setFingerprintError(''); // Clear any previous errors
      },
    );

    return () => {
      fingerprintCaptureListener.remove();
    };
  }, []);

  const handleFingerprintCapture = () => {
    try {
      console.log('................');
      console.log(NativeModules);
      MyFingerprintModule.captureFingerprint();
    } catch (error) {
      console.log(NativeModules);
      console.error('Fingerprint capture error:', error);
      // Display the error message on the screen
      setFingerprintError(`Error is :: ${error}`);
    }
  };

  return (
    <View style={{flex: 1, justifyContent: 'center', alignItems: 'center'}}>
      <Button title="Capture Fingerprint" onPress={handleFingerprintCapture} />
      <Text style={{marginTop: 20}}>
        {fingerprintError
          ? fingerprintError
          : `Fingerprint Data: ${fingerprintData}`}
      </Text>
    </View>
  );
};

export default App;
