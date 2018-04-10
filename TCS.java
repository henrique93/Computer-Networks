import java.io.*;
import java.net.*;
import java.util.Arrays;

class TCS {

	private static int _port = 58030;

	public static void main(String args[]) throws Exception {
		if (args.length > 0) {
			for(int i = 0; i < args.length; i++) {
				if (args[i].equals("-p")) {
					_port = Integer.parseInt(args[++i]);
				}
			}
		}
		DatagramSocket serverSocket = new DatagramSocket(_port);
		byte[] receiveData = new byte[1024];
		byte[] sendData = new byte[1024];
		int numberLanguages = 0;
		File languagesList = new File("languages.txt");
		languagesList.createNewFile();
		BufferedWriter writeBuffer = new BufferedWriter(new FileWriter("languages.txt"));
		BufferedReader readBuffer = new BufferedReader(new FileReader("languages.txt"));

		//PARA TESTAR
		//writeBuffer.write("ingles 192.168.1.3 59100");
		//writeBuffer.flush();
String command = new String();

		while(true) {
			String toSend = new String();
			receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
			command = new String (receivePacket.getData());
			InetAddress IPAddress = receivePacket.getAddress();
			_port = receivePacket.getPort();
			String[] parts = command.split(" ");
			if (parts.length > 0) {
				String[] userPart = command.split("\\r?\\n");
				
				/* TCS - USER */
				/* Solve client's "list" command */
				if (userPart[0].trim().equals("ULQ")) {
					String list = new String();
					String currentLine = new String();
					readBuffer = new BufferedReader(new FileReader("languages.txt"));
					while ((currentLine = readBuffer.readLine()) != null) {
						String[] lang = currentLine.split(" ");
						list += lang[0] + " ";
					}
					toSend = "ULR " + numberLanguages + " " + list.trim() + "\n";
					_port = receivePacket.getPort();
					System.out.println("List: " + IPAddress.getHostName() + " " + _port);
					sendData = toSend.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, _port);
					serverSocket.send(sendPacket);
					readBuffer = new BufferedReader(new FileReader("languages.txt"));
				}

				/*Solve client's "request" command */
				if (parts[0].equals("UNQ") && parts.length >= 2) {
					String currentLine = new String();
					int cont = 1;
					int failed = 1;
					readBuffer = new BufferedReader(new FileReader("languages.txt"));
					while ((currentLine = readBuffer.readLine()) != null) {
						String[] lang = currentLine.split(" ");
						if (lang[0].trim().equals(parts[1].trim())) {
							failed = 0;
							break;
						}
						cont++;
					}
					readBuffer = new BufferedReader(new FileReader("languages.txt"));
					if (failed == 1) {
						toSend = "UNR EOF\n";
					}
					else {
						int languageID = cont;
						String[] split = currentLine.split(" ");
						String IPTRS = split[1];
						String portTRS = split[2];
						toSend = "UNR " + IPTRS + " " + portTRS + "\n";
					}
					_port = receivePacket.getPort();
					System.out.println("Request: " + IPAddress.getHostName() + " " + _port );
					sendData = toSend.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, _port);
					serverSocket.send(sendPacket);
				}
				/* TCS - TRS */
				/*Acknowledge TRS's initialization */
				if (parts[0].equals("SRG")) {
					String status = "NOK";
					String text = new String();
					if (parts.length == 4) {
						String language = parts[1];
						String IPTRS = parts[2];
						String portTRS = parts[3];
						text = language + " " + IPTRS + " " + portTRS;
						writeBuffer.write(text);
						writeBuffer.flush();
						status = "OK";
					}
					System.out.println("+" + text.trim());
					toSend = "SRR " + status + "\n";
					sendData = toSend.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, _port);
					serverSocket.send(sendPacket);
				}

				/* TRS finished translating */
				if (parts[0].equals("SUN")) {
					String status = "NOK";
					if (parts.length == 4) {
						String language = parts[1];
						String IPTRS = parts[2];
						String portTRS = parts[3];

						String toDelete = language + " " + IPTRS + " " + portTRS;
						File tempFile = new File("tempFile.txt");
						BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempFile));
						readBuffer = new BufferedReader(new FileReader("languages.txt"));
						String currentLine;
						while ((currentLine = readBuffer.readLine()) != null) {
						    String trimmedLine = currentLine.trim();
						    if (trimmedLine.equals(toDelete.trim())) {
						    	continue;
						    }
						    else if (!trimmedLine.equals("")){
						    	tempWriter.write(currentLine + System.getProperty("line.separator"));
						    	tempWriter.flush();
						    }
						}
						readBuffer = new BufferedReader(new FileReader("languages.txt"));
						tempWriter.close();
						tempFile.renameTo(languagesList);
						System.out.println("-" + toDelete.trim());
						writeBuffer = new BufferedWriter(new FileWriter("languages.txt", true));
						status = "OK";
					}
					toSend = "SUR " + status + "\n";
					sendData = toSend.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, _port);
					serverSocket.send(sendPacket);
				}	
			}
			command = new String();
		}
	}

}