package com.example.elm327can.obd;


/**
 * Current speed.
 */
public class SpeedObdCommand extends ObdCommand {

  private int metricSpeed = 0;

  public SpeedObdCommand() {
    super("010D");
  }

  @Override
  protected void performCalculations() {
    // Ignore first two bytes [hh hh] of the response.
    metricSpeed = buffer.get(2);
  }

  public String getFormattedResult() {
    return  String.format("%d%s", getSpeedKMpH(), "km/h");
  }

  public int getSpeedKMpH() {
    return metricSpeed;
  }

  public float getSpeed() {
    return metricSpeed / 3.6f;
  }

  @Override
  public String getName() {
    return "OBD2 speed";
  }

}
