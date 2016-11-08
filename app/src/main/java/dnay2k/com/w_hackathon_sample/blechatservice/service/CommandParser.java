package dnay2k.com.w_hackathon_sample.blechatservice.service;

public class CommandParser {

	public static final int COMMAND_NONE = -1;
	public static final int COMMAND_THINGSPEAK = 1;

	private static final String THINGSPEAK_PREFIX = "thingspeak:";
	private static final String COMMAND_SUFFIX = "[*]";
	
	private StringBuilder mReceivedString;
	private int mCommand = COMMAND_NONE;
	private String mParameters;
	
	public CommandParser() {
		mReceivedString = new StringBuilder();
	}
	
	
	public int setString(String message) {
		StringBuilder sb = new StringBuilder();
		sb.append(message);
		return checkCommand();
	}
	
	public int getCommand() {
		return mCommand;
	}
	
	public String getParameterString() {
		return mParameters;
	}
	
	public void resetParser() {
		mCommand = COMMAND_NONE;
		mParameters = null;
	}

	private int checkBLECommand(StringBuilder sb) {
		int prefixIndex = sb.lastIndexOf(THINGSPEAK_PREFIX);
		if(prefixIndex > -1) {
			//Logs.d("# found thingspeak prefix");
			// Check if there is suffix string [*]
			int suffixIndex = sb.lastIndexOf(COMMAND_SUFFIX);
			if(suffixIndex > -1) {
				//Logs.d("# found [*] suffix");
				if(prefixIndex + THINGSPEAK_PREFIX.length() <= suffixIndex) {
					// cut the parameter string from buffer
					mCommand = COMMAND_THINGSPEAK;
					mParameters = mReceivedString.substring(prefixIndex + THINGSPEAK_PREFIX.length(), suffixIndex);
					sb = new StringBuilder();
					//Logs.d("# parameters = "+mParameters);
					return COMMAND_THINGSPEAK;
				}
			}
		} else {

		}

		// Nothing found... Empty buffer if it's too big
		if(sb.length() > 1000) {
			sb = new StringBuilder(sb.substring(sb.length() - 200));
		}

		mCommand = COMMAND_NONE;

		return COMMAND_NONE;
	}

	private int checkCommand() {
		int prefixIndex = mReceivedString.lastIndexOf(THINGSPEAK_PREFIX);
		if(prefixIndex > -1) {
			//Logs.d("# found thingspeak prefix");
			// Check if there is suffix string [*]
			int suffixIndex = mReceivedString.lastIndexOf(COMMAND_SUFFIX);
			if(suffixIndex > -1) {
				//Logs.d("# found [*] suffix");
				if(prefixIndex + THINGSPEAK_PREFIX.length() <= suffixIndex) {
					// cut the parameter string from buffer
					mCommand = COMMAND_THINGSPEAK;
					mParameters = mReceivedString.substring(prefixIndex + THINGSPEAK_PREFIX.length(), suffixIndex);
					mReceivedString = new StringBuilder();
					//Logs.d("# parameters = "+mParameters);
					return COMMAND_THINGSPEAK;
				}
			}
		} else {
			
		}
		
		// Nothing found... Empty buffer if it's too big
		if(mReceivedString.length() > 1000) {
			mReceivedString = new StringBuilder(mReceivedString.substring(mReceivedString.length() - 200));
		}
		
		mCommand = COMMAND_NONE;
		
		return COMMAND_NONE;
	}
	
	
	
	
}
