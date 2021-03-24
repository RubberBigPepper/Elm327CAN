package com.example.elm327can.obd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Base OBD command.
 */
public abstract class ObdCommand 
{
	public static int m_nReadDelay=100;//пауза между посылкой запроса и чтением ответа, в мс
	
	public String m_strLastError="";//последняя ошибка, пусто, если не было
    protected ArrayList<Integer> buffer = null;
    protected String cmd = null;
    protected String rawData = null;

  	protected int m_nReadCount=0;//количество операций чтения
  	protected int m_nErrorCount=0;//сколько ошибок чтения вышло подряд
  	public static int m_nErrorCountReset=100;//через сколько циклов чтения сбрасывать счетчик ошибок
  	public static int m_nErrorCountMax=5;//через сколько подряд ошибок чтения прекращать чтение данных датчика, до сброса

  /**
   * Default ctor to use
   * 
   * @param command
   *          the command to send
   */
  public ObdCommand(String command) {
    this.cmd = command;
    this.buffer = new ArrayList<Integer>();
  }

  /**
   * Prevent empty instantiation
   */
  private ObdCommand() {
  }

  /**
   * Copy ctor.
   * 
   * @param other
   *          the ObdCommand to copy.
   */
  public ObdCommand(ObdCommand other) {
    this(other.cmd);
  }

  /**
   * Sends the OBD-II request and deals with the response.
   *
   * This method CAN be overriden in fake commands.
   *
   * @param in a {@link InputStream} object.
   * @param out a {@link OutputStream} object.
   * @throws IOException if any.
   * @throws InterruptedException if any.
   */
  public boolean run(InputStream in, OutputStream out)
  {
	  m_nReadCount++;
	  if(m_nReadCount>=m_nErrorCountReset)
	  {//сброс счетчиков ошибок
		  m_nErrorCount=0;
		  m_nReadCount=0;
	  }
	  if(m_nErrorCount>=m_nErrorCountMax)
		  return false;
	  try
	  {
		  m_strLastError="";
		  sendCommand(out);
		  readResult(in);
		  m_nErrorCount=0;
		  return true;
	  }
	  catch(Exception ex)
	  {
		  m_strLastError=ex.getMessage();
		  if(m_strLastError==null)
			  m_strLastError="unknown error";
		  m_nErrorCount++;
	  }
	  return false;
  }

  /**
   * Sends the OBD-II request.
   *
   * This method may be overriden in subclasses, such as ObMultiCommand or
   * TroubleCodesObdCommand.
   *
   * @param out
   *          The output stream.
   * @throws IOException if any.
   * @throws InterruptedException if any.
   */
  protected void sendCommand(OutputStream out) throws IOException,
          InterruptedException {
    // write to OutputStream (i.e.: a BluetoothSocket) with an added
    // Carriage return
    out.write((cmd + "\r").getBytes());
    out.flush();

    /*
     * HACK GOLDEN HAMMER ahead!!
     *
     * Due to the time that some systems may take to respond, let's give it
     * 200ms.
     */
    Thread.sleep(m_nReadDelay);
  }

  /**
   * Resends this command.
   *
   * @param out a {@link OutputStream} object.
   * @throws IOException if any.
   * @throws InterruptedException if any.
   */
  protected void resendCommand(OutputStream out) throws IOException,
          InterruptedException {
    out.write("\r".getBytes());
    out.flush();
  }

  /**
   * Reads the OBD-II response.
   * <p>
   * This method may be overriden in subclasses, such as ObdMultiCommand.
   *
   * @param in a {@link InputStream} object.
   * @throws IOException if any.
   */
  protected void readResult(InputStream in) throws Exception {
    readRawData(in);
    checkForErrors();
    fillBuffer();
    performCalculations();
  }

  /**
   * This method exists so that for each command, there must be a method that is
   * called only once to perform calculations.
   */
  protected abstract void performCalculations();

  /**
 * @throws Exception
   *
   */
  protected void fillBuffer() throws Exception {
    rawData = rawData.replaceAll("\\s", "");

    if (!rawData.matches("([0-9A-F]{2})+")) {
      throw new Exception(rawData);
    }

    // read string each two chars
    buffer.clear();
    int begin = 0;
    int end = 2;
    while (end <= rawData.length()) {
      buffer.add(Integer.decode("0x" + rawData.substring(begin, end)));
      begin = end;
      end += 2;
    }
  }

  /**
   * <p>readRawData.</p>
   *
   * @param in a {@link InputStream} object.
   * @throws IOException if any.
   */
  protected void readRawData(InputStream in) throws IOException {
    byte b = 0;
    StringBuilder res = new StringBuilder();

    // read until '>' arrives
    while ((char) (b = (byte) in.read()) != '>')
      res.append((char) b);

    /*
     * Imagine the following response 41 0c 00 0d.
     * 
     * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
     * attention to the fact that I've put the word byte in quotes, because 41
     * is actually TWO bytes (two chars) in the socket. So, we must do some more
     * processing..
     */
    rawData = res.toString().trim();

    /*
     * Data may have echo or informative text like "INIT BUS..." or similar.
     * The response ends with two carriage return characters. So we need to take
     * everything from the last carriage return before those two (trimmed above).
     */
    if(!this.getClass().equals(ObdRawCommand.class))//для всех, кроме команды чистого ответа
    	rawData = rawData.substring(rawData.lastIndexOf(13) + 1);
  }

  void checkForErrors() {
    /*for (Class<? extends ObdResponseException> errorClass : ERROR_CLASSES) {
      ObdResponseException messageError;

      try {
        messageError = errorClass.newInstance();
        messageError.setCommand(this.cmd);
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }

      if (messageError.isError(rawData)) {
        throw messageError;
      }
    }*/
  }

  /**
   * @return the raw command response in string representation.
   */
  public String getResult() {
    return rawData;
  }

  /**
   * @return a formatted command response in string representation.
   */
  public abstract String getFormattedResult();

  /**
   * @return a list of integers
   */
  protected ArrayList<Integer> getBuffer() {
    return buffer;
  }


  /**
   * @return the OBD command name.
   */
  public abstract String getName();

}
