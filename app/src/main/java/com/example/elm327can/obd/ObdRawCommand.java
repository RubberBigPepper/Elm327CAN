
package com.example.elm327can.obd;

/**
 * This method will reset the OBD connection.
 */
public class ObdRawCommand extends ObdCommand {

    /**
     * @param command a {@link String} object.
     */
    public ObdRawCommand(String command) {
        super(command);
    }

    @Override
    public String getFormattedResult() {
        return getResult();
    }

    @Override
    public String getName() {
        return "Custom command " + getName();
    }

	@Override
	protected void performCalculations() {
		// TODO Auto-generated method stub
		
	}

}
