import { NativeModules, Platform } from 'react-native';

const { RNMediaMeta } = NativeModules;

export default {
	get(path, options) {
		if(options && Platform.OS === 'android'){
			return RNMediaMeta.get(path, options)
		} else if (Platform.OS === 'android') {
			return RNMediaMeta.get(path, {'getThumb':true})
		} else {
			return RNMediaMeta.get(path)
		}
	}
}
