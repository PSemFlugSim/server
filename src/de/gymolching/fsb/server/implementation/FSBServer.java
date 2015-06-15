package de.gymolching.fsb.server.implementation;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import de.gymolching.fsb.api.FSBPosition;
import de.gymolching.fsb.server.api.FSBServerInterface;

public class FSBServer implements FSBServerInterface, Runnable
{
	private ServerSocket serverSocket = null;
	private Thread serverThread = null;
	private ArrayList<FSBPosition> positions = null;
	private boolean verbose = false;

	/**
	 * Creates new FSBServer and starts listening
	 * 
	 * @param port
	 *            the port on which this server should listen
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public FSBServer(int port) throws IOException, InterruptedException
	{
		this.verbose = false;

		this.serverSocket = new ServerSocket(port);
		this.serverThread = new Thread(this);
		this.positions = new ArrayList<>();
		this.serverThread.start();

		synchronized (this.positions)
		{
			this.positions.wait();
		}
	}

	/**
	 * Creates new FSBServer and starts listening
	 * 
	 * @param port
	 *            the port on which this server should listen
	 * @param verbose
	 *            whether this server should log it's actions verbosly
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public FSBServer(int port, boolean verbose) throws IOException, InterruptedException
	{
		this.verbose = verbose;

		if (this.verbose)
			System.out.println("[Server] Initializing Server");
		this.serverSocket = new ServerSocket(port);
		this.serverThread = new Thread(this);
		this.positions = new ArrayList<>();
		this.serverThread.start();

		synchronized (this.positions)
		{
			if (this.verbose)
				System.out.println("[Server] Waiting for Server to be fully started...");
			this.positions.wait();
		}
	}

	public void stop() throws InterruptedException
	{
		this.serverThread.interrupt();
		this.serverThread.join();
	}

	public FSBPosition getMostRecentPositionUpdate() throws InterruptedException
	{

		if (this.verbose)
			System.out.println("[Server] Trying to get most recent position update");

		boolean shouldWait = false;
		do
		{
			shouldWait = false;
			synchronized (this.positions)
			{
				if (this.positions.size() == 0)
				{
					shouldWait = true;

					if (this.verbose)
						System.out.println("[Server] No new position received yet. Staying Idle");

					this.positions.wait();
				}
			}
		}
		while (shouldWait == true);

		synchronized (this.positions)
		{
			FSBPosition mostRecentPosition = this.positions.get(this.positions.size() - 1);
			this.positions.clear();

			if (this.verbose)
				System.out.println("[Server] Returning newly received position");
			return mostRecentPosition;
		}
	}

	public void run()
	{
		// Notify main thread that server is started and waiting to accept connection
		synchronized (this.positions)
		{
			if (this.verbose)
				System.out.println("[Server] Listening for incoming client connection attempts");
			this.positions.notifyAll();
		}

		while (!Thread.interrupted())
		{
			// Wait for incoming connection and
			try
			{
				Socket connSocket = this.serverSocket.accept();
				DataInputStream dis = new DataInputStream(connSocket.getInputStream());
				if (this.verbose)
					System.out.println("[Server] Client connected " + connSocket.getInetAddress() + ":" + connSocket.getPort());

				while (connSocket.isConnected())
				{
					// Receive Input from client and store it in our positions list
					String connInputString = null;
					try
					{
						connInputString = dis.readUTF();
					}
					catch (EOFException e)
					{
//						e.printStackTrace();
						break;
					}

					if (this.verbose)
						System.out.println("[Server] Received new position: " + connInputString);

					synchronized (this.positions)
					{
						this.positions.add(new FSBPosition(connInputString));
						this.positions.notifyAll();
					}
				}

				if (this.verbose)
					System.out.println("[Server] Client disconnected. Closing socket and listening for new Connection...");

				dis.close();
				connSocket.close();
			}
			catch (IOException e)
			{
//				e.printStackTrace();
				
				try
				{
					this.serverSocket.close();
				}
				catch (IOException f)
				{
					f.printStackTrace();
				}
			}
		}
	}
}
