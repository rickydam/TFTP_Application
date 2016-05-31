// TFTPClient.java
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPClient {
	
	//SIM PORT
	private static final int simPort = 23;
	
	//MAX DATA PER PACKET
	public static final int DATA_SIZE = 512;
	
	//msg array size
	public static final int TOTAL_SIZE = DATA_SIZE+4;
	
	private static final int DATA_PACKET = 3;
	private static final int ACK_PACKET = 4;
	
	private static final int TIMEOUT = 1000;
	private static final int RETRANSMIT_TIME = 2000;
	
	//REQUEST AND MODE TYPES
	public static enum Request { READ, WRITE, ERROR};
	public static enum Mode { NORMAL, TEST};
	
	//packets and sockets
	private DatagramPacket sendPacket, receivePacket;
	private DatagramSocket sendReceiveSocket;
	private String contents;

	//ACK counter
	private Byte ackCntL, ackCntR;//starting byte

	
   	public TFTPClient()
   	{
   		try {
			sendReceiveSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
	}
   	
   	public void runClient(Scanner readInput) {
   		Scanner re = readInput;
   		//String filepath, workingDir; -- To implement
   		String filename;
   		String mode;
   		Request req;
   		
   		int cmd = 0;
   		
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
   		
   		//read in filename
   		while(true) {
			try {
				System.out.print("Enter the file you would like to " + req.toString() +": ");
				filename = re.nextLine();
				//workingDir = System.getProperty("user.dir");
				//filepath = workingDir + "\\" + filename;
				File input = new File(filename);
				Scanner read = new Scanner(input);//used to verify if file is valid
				break;
			} catch(FileNotFoundException e) {
				System.out.println("File does not exist.  Please enter a valid file.");
			}
		}
   		
   		//*****************TO FIX******************
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
   		//for(;;) {
   			run(filename, mode, req);
   		//}
   	}

   	public void run(String fp, String filemode, Request r)
   	{
   			
   			String filepath = fp;
   			String mode = filemode;
   			Request req = r;
			
			byte[] msg = new byte[TOTAL_SIZE];
			
			msg[0] = 0;
			if(req==Request.READ) {
				msg[1] = 1;
				
			} else if(req==Request.WRITE) {
				msg[1] = 2;
			}
			
			
			int index = 2;
			//put file name into bytes
			byte[] fn = filepath.getBytes();
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
				sendPacket = new DatagramPacket(msg, index, InetAddress.getLocalHost(), simPort);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("\nClient: Sending packet to simulator.");
	        System.out.println("To host: " + sendPacket.getAddress());
	        System.out.println("Destination host port: " + sendPacket.getPort());
	        int packetLength = sendPacket.getLength();
	        System.out.println("Packet Length: " + packetLength);
	        System.out.println("Contents(bytes): " + msg);
	        String contents = new String(msg,2,packetLength);
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
	        
	        //send request
			try{
            	sendReceiveSocket.send(sendPacket);
            } catch (IOException e) {
            	e.printStackTrace();
                System.exit(1);
            }
			
			if(req==Request.READ) {
				read(filepath);
			} else if(req==Request.WRITE) {
				write(filepath);
			}
	}
   	
   	public void read(String fp)
   	{
   		String filepath = fp;
   		Byte blocknum1=0;
		Byte blocknum2=1;
		ackCntL = 0;
		ackCntR = 1;//we are starting our read ack counter at 1
		
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("new"+filepath));
			for(;;) {
				int len;
				byte[] msg = new byte[TOTAL_SIZE];
				byte[] data = new byte[DATA_SIZE];

				receivePacket = new DatagramPacket(msg, msg.length);

				try {
					/*
					 * Network Error Handling 
					 * ACK retransmission is not needed as duplicate ack packets are disabled
					 * Only have check for initial timeout
					 */
					for(;;) {
						//set timeout time

						//*******ADD MULTIPLE SENDS BEFORE TIME OUT*********************************************
						sendReceiveSocket.setSoTimeout(TIMEOUT);
						
						//block socket, wait for packet
						sendReceiveSocket.receive(receivePacket);

						//****ERROR HANDLING: DUPLICATE DATA****
						
						//if incoming block number != ack counter + 1, keep waiting, 
						//check for duplicate
						//byte byteCheck1 = (byte) (ackCntR+1);
						//case where right ack byte counter is at max
						
						//get received packet number
						Byte leftByte = new Byte(receivePacket.getData()[2]);
						Byte rightByte = new Byte(receivePacket.getData()[3]);
											
						//compare block number to our counter
						if(leftByte.compareTo(ackCntL) == 0 && rightByte.compareTo(ackCntR) == 0) {
							//increment ack counter if correct block number received
							if(ackCntL.intValue()<256) {
								if(ackCntR.intValue() == 255) {
									ackCntL++;
									ackCntR=0;
								} else {
									ackCntR++;
								}
							} else {
								System.out.println("File too big, exiting program.");
							}
							
							break;
						} else {
							System.out.println("Packet not as expected - error cannot be handled this iteration");
							System.exit(1);
						}
						/**/
					}
				} catch (IOException e) {//CHANGE TO SEND DATA MORE THAN 5 TIMES
					System.out.println("No data received: Data lost.");
					System.out.println("Shutting down.");
					System.exit(1);
				}
				
				try {
		             Thread.sleep(500);
		        } catch (InterruptedException e) {
		        	 e.printStackTrace();
		        }
				
				//get received packet block number
				Byte leftByte = new Byte(receivePacket.getData()[2]);
				Byte rightByte = new Byte(receivePacket.getData()[3]);
				
				System.out.println("Client: DATA Packet received from simulator.");
		        System.out.println("From host: " + receivePacket.getAddress());
		        System.out.println("Host port: " + receivePacket.getPort());
		        len = receivePacket.getLength();
		        System.out.println("Packet Length: " + len);
		        System.out.println("Block Number: " + leftByte.toString() + rightByte.toString());
		        System.out.println("Contents(bytes): " + msg);
		        String contents = new String(msg,4,len-4);
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
				ack[1] = ACK_PACKET;
				ack[2] = blocknum1;
				ack[3] = blocknum2;

				sendPacket = new DatagramPacket(ack, ack.length, InetAddress.getLocalHost(), 23);
				
				System.out.println("Client: Sending ACK packet to simulator.");
		        System.out.println("To host: " + sendPacket.getAddress());
		        System.out.println("Destination host port: " + sendPacket.getPort());
		        int packetLength = sendPacket.getData().length;
		        System.out.println("Packet Length: " + packetLength);
		        System.out.println("Block Number: " + leftByte.toString() + rightByte.toString());
		        System.out.println("Contents(bytes): " + ack);
		        
		        if(packetLength > 4) {
		        	// It is not an ACK packet
		        	contents = new String(ack, 4, DATA_SIZE);
		        	System.out.println("Contents(string): \n" + contents + "\n");
		        }
		        else {
		        	// It is an ACK packet
		        	System.out.println("Contents(string): \n" + "########## ACKPacket ##########\n");
		        }
		        
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
		        
		        //send 
				try {
					sendReceiveSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				//if this is he last data packet, end
				if(len<DATA_SIZE) {
					out.close();
					
					try {
			             Thread.sleep(2500);
			        } catch (InterruptedException e) {
			        	 e.printStackTrace();
			        }
					
					System.out.println("#####  OPERATION COMPLETED.  #####" + "\n");
					/*
					 * IMPLEMENT RE-PROMPT FOR NEW FILE TRANSFER
					 */
					System.exit(1);
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
   	
public void write(String fp) {
		
		String filepath = fp;
		Byte blocknum1= new Byte((byte)0);
		Byte blocknum2= new Byte((byte)0);//first block sent
		ackCntL = 0;
		ackCntR = 0;//start ack counter at 0
		int len, dataCheck;
		
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filepath));
			do {
				byte[] msg = new byte[TOTAL_SIZE]; // msg has size 516
				byte[] data = new byte[DATA_SIZE]; // data has size 512
				int i = 0;//DATA DELAY TIMER

				receivePacket = new DatagramPacket(msg, msg.length);
				
				try {
					// Network Error Handling 
					
					for(;;) {
						sendReceiveSocket.receive(receivePacket);
						/*
						while(true) {
							try {
								//sendReceiveSocket.setSoTimeout(TIMEOUT);
								
								break;
							} catch (SocketTimeoutException e) {
								sendReceiveSocket.send(sendPacket);
							}
						}
						*/
						//set timeout time

						//****ERROR HANDLING: DATA LOSS****
						
						
						//block socket, wait for packet
						
						//****ERROR HANDLING: DUPLICATE DATA****
						
						//if incoming block number != ack counter + 1, keep waiting, 
						//check for duplicate
						/*
						byte byteCheck1 = (byte) (ackCntR+1);
						//case where right ack byte counter is at max
						if(ackCntR==255) {
							byte leftCount = (byte) (ackCntL+1);
							if(receivePacket.getData()[2]==leftCount && receivePacket.getData()[3]==byteCheck1) {
								break;
							}
						}else if(receivePacket.getData()[2]==ackCntL && receivePacket.getData()[3]==byteCheck1) {
							break;
						} else {
							System.out.println("Packet not as expected - error cannot be handled this iteration");
							System.exit(1);
						}
						*/
						
						//Get block number from received packet to compare
						Byte leftByte = new Byte(receivePacket.getData()[2]);
						Byte rightByte = new Byte(receivePacket.getData()[3]);
						
						//If ack counter matches packet block number, continue, else break
						if(leftByte.compareTo(ackCntL) == 0 && rightByte.compareTo(ackCntR) == 0) {
							//increment ack counter if correct block number received
							if(ackCntL.intValue()<256) {
								if(ackCntR.intValue() == 255) {
									ackCntL++;
									ackCntR=0;
								} else {
									ackCntR++;
								}
							} else {
								System.out.println("File too big, exiting program.");
							}
							
							break;
						} else {
							System.out.println("Packet not as expected - error cannot be handled this iteration");
							System.exit(1);
						}
					}//end for
					
				} catch (IOException e) {
					System.out.println("No data received: Data lost.");
					System.out.println("Shutting down.");
					System.exit(1);
				}

				
				Byte leftByte = new Byte(receivePacket.getData()[2]);
				Byte rightByte = new Byte(receivePacket.getData()[3]);
				String contents;
				
				System.out.println("Client: Packet received from simulator.");
		        System.out.println("From host: " + receivePacket.getAddress());
		        System.out.println("Host port: " + receivePacket.getPort());
		        int packetLength = receivePacket.getLength();
		        System.out.println("Length: " + packetLength);
		        System.out.println("Block Number: " + leftByte.toString() + rightByte.toString());
		        System.out.println("Contents(bytes): " + msg);
		        if(packetLength > 4) {
		        	// It is not an ACK packet
		        	contents = new String(msg, 4, DATA_SIZE);
		        	System.out.println("Contents(string): \n" + contents + "\n");
		        }
		        else {
		        	// It is an ACK packet
		        	System.out.println("Contents(string): \n" + "########## ACKPacket ##########\n");
		        }

		        
		        if (blocknum1 < 256) {
					if(blocknum2 == 255) {
						blocknum1++;
						blocknum2 = 0;
					} else {
						blocknum2++;
					}
		        } else {
		        	System.out.println("Maximum memory reached.  Aborting...");
		        	System.exit(1);
		        }
		        
		        //
		        dataCheck = in.read(data);
				if(dataCheck==-1) {
					len = 0;
				} else {
					len = dataCheck;
				}

				msg[0] = 0;
				msg[1] = DATA_PACKET;
				msg[2] = blocknum1;
				msg[3] = blocknum2;
				
			  //System.arraycopy(src, srcLoc, dest, destLoc, len)
				if(len != 0) {
					System.arraycopy(data, 0, msg, 4, len);
				}
					
				sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), simPort);
				
				System.out.println("Client: Sending packet to simulator.");
		        System.out.println("To host: " + sendPacket.getAddress());
		        System.out.println("Destination host port: " + sendPacket.getPort());
		        packetLength = sendPacket.getLength();
		        System.out.println("Packet Length: " + packetLength);
		        System.out.println("Block Number: " + blocknum1.toString() + blocknum2.toString());
		        System.out.println("Contents(bytes): " + msg);
		        if(packetLength > 4) {
		        	// It is not an ACK packet
		        	contents = new String(msg, 4, DATA_SIZE);
		        	System.out.println("Contents(string): \n" + contents + "\n");
		        }
		        else {
		        	// It is an ACK packet
		        	System.out.println("Contents(string): \n" + "########## ACKPacket ##########\n");
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
		Scanner re = new Scanner(System.in);
		for(;;) {
			c.runClient(re);
		}
	}
	
	private void testString(String n) {
		System.out.println(n);
	}
}
