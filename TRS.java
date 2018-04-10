import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;


public class TRS {

	private static int _TRSport = 59000;
	private static String TCSname = new String();
	private static int _TCSport = 58030;
	private static String _name = new String();
	public static int exit = 0;

	public static void main(String args[]) throws Exception {

		final String language = args[0];
		String[] translate = new String[20];
		String toTranslate = new String();
		String translated = new String();
		String mess = new String();
		_name = Inet4Address.getLocalHost().getHostAddress();
		
		int start = 1;
		int on = 0;
		
		ServerSocket socket = new ServerSocket();
		DatagramPacket sendPacket;
		DatagramPacket receivePacket;

		String fileName = new String();
		BufferedReader readBuffer;
		
		if (args.length > 1) {
			for(int i = 1; i < args.length; i++) {
				if (args[i].equals("-p")) {
					_TRSport = Integer.parseInt(args[++i]);
				}
				if (args[i].equals("-n")) {
					TCSname = args[++i];
				}
				if (args[i].equals("-e")) {
					_TCSport = Integer.parseInt(args[++i]);
				}
			}
		}
		else {
			TCSname = InetAddress.getLocalHost().getHostName();
		}
		while (on != 1) {
			try {
				socket = new ServerSocket(_TRSport);
				on = 1;
			} catch (Exception e) { _TRSport += 1; }
		}
		
		Thread inputThread = new Thread(new Runnable() {
			@Override
			public void run() {
	            Scanner scan = new Scanner(System.in);
	            String input = new String();
	            byte[] message = new byte[1024];
				byte[] buffer = new byte[1024];
				String receivedMessage = new String();
	            while (true) {
	                input = scan.nextLine();
	                if (input.equals("exit")) {
	                	try {
	                		InetAddress IPAddress = InetAddress.getByName(TCSname);
	                		DatagramSocket clientSocket = new DatagramSocket();
	                		String mess = "SUN " + language + " " + _name + " " + Integer.toString(_TRSport) + "\n";
	        				message = mess.getBytes();
	        				DatagramPacket sendPacket = new DatagramPacket(message, message.length, IPAddress, _TCSport);
	        				clientSocket.send(sendPacket);
	        				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
	        				clientSocket.receive(receivePacket);
	        				receivedMessage = new String(receivePacket.getData());
	        				if (receivedMessage.equals("NOK")) {
	        					//what???
	        				}
	        				clientSocket.close();
	        				System.exit(0);
	                	} catch (UnknownHostException e) {}
	                	catch (SocketException e) {}
	                	catch (IOException e) {}
	                }
	            }
	        }
	    });
		
		inputThread.start();
		
		InetAddress IPAddress = InetAddress.getByName(TCSname);
		DatagramSocket clientSocket = new DatagramSocket();
		Scanner scanner = new Scanner(System.in);
		
		while(true) {
			/* TRS - TCS */
			/* Inform TCS which language this TRS translates */
			byte[] message = new byte[1024];
			byte[] buffer = new byte[1024];
			String receivedMessage = new String();
			if (start == 1) {
				mess = "SRG " + language + " " + _name + " " + Integer.toString(_TRSport) + "\n";
				message = mess.getBytes();
				sendPacket = new DatagramPacket(message, message.length, IPAddress, _TCSport);
				clientSocket.send(sendPacket);
				receivePacket = new DatagramPacket(buffer, buffer.length);
				clientSocket.receive(receivePacket);
				receivedMessage = new String(receivePacket.getData());
				if (receivedMessage.equals("NOK")) {
					//what???
				}
				start = 0;
			}

			Socket connectionSocket = socket.accept();
			BufferedReader fromUser = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			DataOutputStream toUser = new DataOutputStream(connectionSocket.getOutputStream());
			String command = fromUser.readLine();
			String[] parts = command.split(" ");
			if (parts.length > 0) {
				/* TRS - USER */
				/* Translate client's text or file */
				if (parts[0].equals("TRQ") && parts.length > 3) {
					String TorF = parts[1];
					if (TorF.equals("t")) {
						fileName = language + "_translation.txt";
						readBuffer = new BufferedReader(new FileReader(fileName));
						int wordNumber = Integer.parseInt(parts[2]);
						int failedTranslation = 1;
						translate = Arrays.copyOfRange(parts, 3, parts.length);
						toTranslate = Arrays.toString(translate);
						toTranslate = toTranslate.substring(1, toTranslate.length() - 1);
						toTranslate = toTranslate.replaceAll("\\,","");
						System.out.println(TCSname + " " + _TCSport + ": " + toTranslate);
						for (int i = 3; i < wordNumber + 3; i++) {
							String currentLine = new String();
							while ((currentLine = readBuffer.readLine()) != null){
								String[] lang = currentLine.split(" ");
								if (lang[0].trim().equals(parts[i].trim())) {
									translated += lang[1] + " ";
									failedTranslation = 0;
								}
							}
							readBuffer = new BufferedReader(new FileReader(fileName));
						}
						if (failedTranslation == 1) {
							toUser.writeBytes("TRR NTA\n");
						}
						else {
							toUser.writeBytes("TRR " + TorF + " " + wordNumber + " " + translated.trim() + "\n");
						}
						//para contar numero de palavras que foram para traduçao
						StringTokenizer st = new StringTokenizer(translated);
						System.out.println(translated + " " + "(" + st.countTokens()  + ")");
						translated = new String();
						toTranslate = new String();
					}
					else if (TorF.equals("f")) {
						//Create file to translate
						String[] dataToConvert = Arrays.copyOfRange(parts, 4, parts.length);
						byte[] bytes = new byte[dataToConvert.length];
						for(int i = 0 ; i < bytes.length ; ++i) {
						    bytes[i] = Byte.parseByte(dataToConvert[i]);
						}
						FileOutputStream fs = new FileOutputStream(parts[2]);
						fs.write(bytes);
						fs.close();
						
						int failedTranslation = 1;
						String currentLine = new String();
						translated = new String();
						fileName = language + "_file_translation.txt";
						readBuffer = new BufferedReader(new FileReader(fileName));
						String name = parts[2];
						int size = Integer.parseInt(parts [3]);
						while ((currentLine = readBuffer.readLine()) != null){
							String[] lang = currentLine.split(" ");
							if (lang[0].trim().equals(name.trim())) {
								translated = lang[1];
								failedTranslation = 0;
							}
						}
						if (failedTranslation == 1) {
							toUser.writeBytes("TRR NTA\n");
						}
						else {
							File file = new File(translated);
							long fileSize = file.length();
							System.out.println(fileSize + " bytes to transmit");
							Path path = Paths.get(file.getAbsolutePath());
							byte[] data = Files.readAllBytes(path);
							String dataString = Arrays.toString(data).replace(",", "");
							dataString = dataString.substring(1, dataString.length() - 1);
							toUser.writeBytes("TRR " + TorF + " " + translated + " " + fileSize + " " + dataString + "\n");
						}
					}
				}
			}
			translated = new String();
			parts = new String[1024];
		}
	}
	
}