import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.Arrays;
import java.awt.image.*;
import javax.imageio.ImageIO;

class User {

	private static String TCSname = new String();
	private static int TCSport = 58030;

	public static void main(String args[]) throws Exception {
		if (args.length > 0) {
			for(int i = 0; i < args.length; i++) {
				if (args[i].equals("-n")) {
					TCSname = args[++i];
				}
				if (args[i].equals("-p")) {
					TCSport = Integer.parseInt(args[++i]);
				}
			}
		}
		else {
			TCSname = InetAddress.getLocalHost().getHostName();
		}
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName(TCSname);
		Socket TcpClientSocket = new Socket();
		byte[] message = new byte[1024];
		byte[] buffer = new byte[1024];
		String command = inFromUser.readLine();
		String receivedMessage = new String();
		String[] languagesAvailable = new String[1024];
		String[] translated = new String[1024];
		while (!command.equals("exit")) {
			String[] parts = command.split(" ");

			//List command   CLIENT - TCS
			if (parts[0].equals("list")) {
				message = "ULQ\n".getBytes();
				DatagramPacket sendPacket = new DatagramPacket(message, message.length, IPAddress, TCSport); //port 58030
				clientSocket.send(sendPacket);
				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
				clientSocket.receive(receivePacket);
				receivedMessage = new String(receivePacket.getData());
				languagesAvailable = receivedMessage.split(" ");
				languagesAvailable = Arrays.copyOfRange(languagesAvailable, 2, languagesAvailable.length);
				int i = 1;
				for(String s: languagesAvailable) {
					if (s.trim().length() < 1) {
						System.out.println("Nao existem servidores de traducao ligados");
					}
					else {
						System.out.println(i++ + "- " + s);
					}
				}
			}

			// REQUEST COMMAND
			else if ((parts[0].equals("request")) && (parts.length >= 4)) { //ver se o comando esta certo
				int lang = Integer.parseInt(parts[1]);
				String TorF = parts [2];
				String filename = parts[3];
				String[] toTranslate = parts;
				toTranslate = Arrays.copyOfRange(toTranslate, 3, toTranslate.length);


				//CLIENT - TCS
				String mess = "UNQ " + languagesAvailable[lang - 1] + "\n";
				message = mess.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(message, message.length, IPAddress, TCSport);
				clientSocket.send(sendPacket);
				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
				clientSocket.receive(receivePacket);
				receivedMessage = new String(receivePacket.getData());


				//CLIENT - TRS
				// Falar com o TRS com os dados recebidos do TCS para pedir a traducao
				parts = receivedMessage.split(" ");
				InetAddress IPTRS = InetAddress.getByName(parts[1]);
				int portTRS = Integer.parseInt(parts[2].trim());
				String nameIP = IPTRS.getHostName();
				String serverResponse = new String();
				String translate = new String();
				if (TorF.equals("t")) {
					translate = Arrays.toString(toTranslate);
					translate = translate.replace(",", "");
					mess = "TRQ " + TorF + " " + toTranslate.length + " " + translate.substring(1, translate.length() - 1) + "\n";
	
					//TCP
					TcpClientSocket = new Socket(IPTRS, portTRS);
					DataOutputStream outToServer = new DataOutputStream(TcpClientSocket.getOutputStream());
					BufferedReader inFromServer = new BufferedReader(new InputStreamReader(TcpClientSocket.getInputStream()));
					outToServer.writeBytes(mess);
					serverResponse = inFromServer.readLine();
				}
				if (TorF.equals("f")) {
					File file = new File(filename);
					long size = file.length();
					System.out.println(size + " bytes to transmit");
					Path path = Paths.get(file.getAbsolutePath());
					byte[] data = Files.readAllBytes(path);
					String dataString = Arrays.toString(data).replace(",", "");
					dataString = dataString.substring(1, dataString.length() - 1);
					mess = "TRQ " + TorF + " " + filename + " " + size + " " + dataString + "\n";
					TcpClientSocket = new Socket(IPTRS, portTRS);
					DataOutputStream outToServer = new DataOutputStream(TcpClientSocket.getOutputStream());
					BufferedReader inFromServer = new BufferedReader(new InputStreamReader(TcpClientSocket.getInputStream()));
					outToServer.writeBytes(mess);
					serverResponse = inFromServer.readLine();
				}
				
				if (serverResponse.trim().equals("TRR NTA")){
					System.out.println("Traducao inexistente");
				}
				if (serverResponse.trim().equals("TRR ERR")){
					System.out.println("ERRO NA TRADUCAO");
				}
				parts = serverResponse.split(" ");
				if (parts[0].equals("TRR") && parts.length > 3){
					if (parts[1].equals("t")) {
						translated = parts;
						translated = Arrays.copyOfRange(translated, 3, translated.length);
						translate = Arrays.toString(translated);
						parts = nameIP.trim().split("\\.");
						System.out.println(parts[0] + " " + portTRS);
						System.out.println(parts[0] + ": " + translate.substring(1, translate.length() - 1));
					}
					else if (parts[1].equals("f")) {
						String[] data = Arrays.copyOfRange(parts, 4, parts.length);
						byte[] bytes = new byte[data.length];
						for(int i = 0 ; i < bytes.length ; ++i) {
						    bytes[i] = Byte.parseByte(data[i]);
						}
						FileOutputStream fs = new FileOutputStream(parts[2]);
						fs.write(bytes);
						fs.close();
						System.out.println("received file " + parts[2] + " " + parts[3] + " Bytes");
					}
				}
			}
			parts = new String[1024];
			buffer = new byte[1024];
			command = inFromUser.readLine();
		}
		TcpClientSocket.close();
		clientSocket.close();
	}

}