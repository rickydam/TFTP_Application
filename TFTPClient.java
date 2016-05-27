// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPClient {
	public static final int DATA_SIZE = 512;
	public static final int TOTAL_SIZE = DATA_SIZE+4;
	public static enum Request { READ, WRITE, ERROR};
	public static enum Mode { NORMAL, TEST};
	public Request req;
	private String filename;
	private String mode;
	
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	private String contents;

   	public TFTPClient()
   	{
   		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}

   	public void run()
   	{
		Scanner re = new Scanner(System.in);
		int cmd = 0;

		for(;;) {
			while(true) {
				try {
					System.out.print("[1]Read   [2]Write  [5]Shutdown : ");
					cmd = Integer.parseInt(re.nextLine());
					if(cmd == 1) {
						req = Request.READ;
						break;
					} else if(cmd == 2) {
						req = Request.WRITE;
						break;
					} else if (cmd == 5) {
						System.exit(1);
					}
				} catch(NumberFormatException e) {
					System.out.println("Please enter a valid option");
				}
			}

			while(true) {
				try {
					System.out.print("Filename : ");
					filename = re.nextLine();
					File input = new File(filename);
					Scanner read = new Scanner(input);
					break;
				} catch(FileNotFoundException e) {
					System.out.println("Please enter a valid filename");
				}
			}
			while(true) {
				try {
					System.out.print("[1]netascii  [2]octet : ");
					cmd = Integer.parseInt(re.nextLine());
					if(cmd == 1) {
						mode = "netascii";
						break;
					} else if(cmd == 2) {
						mode = "octet";
						break;
					}
				} catch(NumberFormatException e) {
					System.out.println("Please enter a valid option");
				}
			}
			mode = "octet";
			byte[] msg = new byte[TOTAL_SIZE];
			
			msg[0] = 0;
			if(req==Request.READ) {
				msg[1] = 1;
				
			} else if(req==Request.WRITE) {
				msg[1] = 2;
			}
			int index = 2;
			
			byte[] fn = filename.getBytes();
			System.arraycopy(fn,0,msg,index,fn.length);
			index = index + fn.length;
			msg[index] = 0;
			index++;
			
			byte[] md = mode.getBytes();
			System.arraycopy(md,0,msg,index,md.length);
			index = index + md.length;
			msg[index] = 0;
			index++;

			try {
				sendPacket = new DatagramPacket(msg, index, InetAddress.getLocalHost(), 23);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("\nClient: Sending packet to simulator.");
	        System.out.println("To host: " + sendPacket.getAddress());
	        System.out.println("Destination host port: " + sendPacket.getPort());
	        int packetLength = sendPacket.getLength();
	        System.out.println("Length: " + packetLength);
	        System.out.println("Contents(bytes): " + msg);
	        String contents = new String(msg,0,packetLength);
	        System.out.println("Contents(string): " + contents + "\n");
			
	        try {
	             Thread.sleep(500);
	        } catch (InterruptedException e) {
	        	 e.printStackTrace();
	        }
	        
	        System.out.println("Client: Waiting for packet from simulator............" + "\n");
	        
	        try {
	             Thread.sleep(500);
	         } catch (InterruptedException e) {
	        	 e.printStackTrace();
	         }
	        
			try{
            	sendReceiveSocket.send(sendPacket);
            } catch (IOException e) {
            	e.printStackTrace();
                System.exit(1);
            }
			
			if(req==Request.READ) {
				read();
			} else if(req==Request.WRITE) {
				write();
			}
		} 
	}
   	
   	public void read()
   	{
		byte blocknum1=0;
		byte blocknum2=1;
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("read.txt"));
			for(;;) {
				int len;
				byte[] msg = new byte[TOTAL_SIZE];
				byte[] data = new byte[DATA_SIZE];

				receivePacket = new DatagramPacket(msg, msg.length);

				try {
					sendReceiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
				
				System.out.println("Client: Packet received from simulator.");
		        System.out.println("From host: " + receivePacket.getAddress());
		        System.out.println("Host port: " + receivePacket.getPort());
		        len = receivePacket.getLength();
		        System.out.println("Length: " + len);
		        System.out.println("Contents(bytes): " + msg);
		        String contents = new String(msg,0,len);
		        System.out.println("Contents(string): \n" + contents + "\n");
                
		        try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
		        
		      //System.arraycopy(src, srcLoc, dest, destLoc, len)
				System.arraycopy(receivePacket.getData(), 4, data, 0, receivePacket.getLength()-4);

				for(len = 4; len < data.length; len++) {
					if (data[len] == 0) break;
				}

				out.write(data,0,len);

				byte[] ack = new byte[4];
				ack[0] = 0;
				ack[1] = 4;
				ack[2] = blocknum1;
				ack[3] = blocknum2;

				sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), 23);
				
				System.out.println("Client: Sending packet to simulator.");
		        System.out.println("To host: " + sendPacket.getAddress());
		        System.out.println("Destination host port: " + sendPacket.getPort());
		        int packetLength = sendPacket.getLength();
		        System.out.println("Length: " + packetLength);
		        System.out.println("Contents(bytes): " + ack);
		        contents = new String(ack, 0, packetLength);
		        System.out.println("Contents(string): " + contents + "\n");
		        
		        try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
		        
		        System.out.println("Client: Waiting for packet from simulator............" + "\n");
		        
		        try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
		        
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }

				if(len<DATA_SIZE) {
					out.close();
					System.out.println("#####  OPERATION COMPLETED.  #####" + "\n");
					try {
			             Thread.sleep(1000);
			        } catch (InterruptedException e) {
			        	 e.printStackTrace();
			        }
					break;
				}

				if(blocknum2 == 255) {
					blocknum1++;
				}
				blocknum2++;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
   	
	public void write() {
		
		byte blocknum1=0;
		byte blocknum2=1;
		int len;
		
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream("read.txt"));
			do {
				byte[] msg = new byte[TOTAL_SIZE]; // msg has size 516
				byte[] data = new byte[DATA_SIZE]; // data has size 512

				receivePacket = new DatagramPacket(msg, msg.length);

				try {
					sendReceiveSocket.receive(receivePacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
				
				System.out.println("Client: Packet received from simulator.");
		        System.out.println("From host: " + receivePacket.getAddress());
		        System.out.println("Host port: " + receivePacket.getPort());
		        int packetLength = receivePacket.getLength();
		        System.out.println("Length: " + packetLength);
		        System.out.println("Contents(bytes): " + msg);
		        String contents = new String(msg,0,packetLength);
		        System.out.println("Contents(string): " + contents + "\n");
				
		        try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
		        
				if(blocknum2 == 255) {
					blocknum1++;
				}
				blocknum2++;
				
				len = in.read(data);
				
				msg[0] = 0;
				msg[1] = 3;
				msg[2] = blocknum1;
				msg[3] = blocknum2;

				System.arraycopy(data, 0, msg, 4, len);
					
				sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 23);
				
				System.out.println("Client: Sending packet to simulator.");
		        System.out.println("To host: " + sendPacket.getAddress());
		        System.out.println("Destination host port: " + sendPacket.getPort());
		        packetLength = sendPacket.getLength();
		        System.out.println("Length: " + packetLength);
		        System.out.println("Contents(bytes): " + msg);
		        contents = new String(msg, 0, packetLength);
		        System.out.println("Contents(string): \n" + contents + "\n");
				
		        try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
		        
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e){
					e.printStackTrace();
					System.exit(1);
			}
				
		} while (len==DATA_SIZE);
			if(len<DATA_SIZE) {
				in.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String args[])
	{
		TFTPClient c = new TFTPClient();
		c.run();
	}
}