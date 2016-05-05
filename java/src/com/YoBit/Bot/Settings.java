package com.YoBit.Bot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Settings
{
	public static String getUserDataDirectory()
	{
		String path = System.getProperty("user.home") + File.separator + ".yobitbot" + File.separator;
		File directory = new File(path);
		if (!directory.exists())
		{
			try
			{
				directory.mkdir();
			}
			catch (SecurityException se)
			{
				//handle it
			}
		}
		return path;
	}

	public static int getNoonce()
	{
		return getNoonce(true);
	}

	public static String get(String file)
	{
		BufferedReader br;
		String key = "";
		try
		{
			br = new BufferedReader(new FileReader(getUserDataDirectory() + file));

			try
			{
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null)
				{
					sb.append(line);
					line = br.readLine();
				}
				key = sb.toString();

			}
			catch (IOException e)
			{
			}
			finally
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
				}
			}

		}
		catch (FileNotFoundException e)
		{

		}

		return key;
	}

	public static void put(String file, String text)
	{
		Writer writer = null;

		try
		{
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getUserDataDirectory() + file), "utf-8"));
			writer.write(text);
		}
		catch (IOException ex)
		{
			// report
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch (Exception ex)
			{
				/*ignore*/
			}
		}
	}

	public static int getNoonce(boolean increase)
	{
		int noonce = 0;
		BufferedReader br;

		if (!MainWindow.textFieldNoonce.equals(""))
		{
			try
			{
				noonce = Integer.parseInt(MainWindow.textFieldNoonce.getText());
			}
			catch (NumberFormatException e)
			{
				noonce = 0;
			}
		}

		if (noonce >= Integer.MAX_VALUE)
		{
			MainWindow.doStop("Noonce value has reached maximum. You must create a new YoBIT API Key and Secret pair and reset NoOnce to 0.");
		}

		if (noonce == 0)
		{
			try
			{
				br = new BufferedReader(new FileReader(getUserDataDirectory() + "noonce.txt"));

				try
				{
					StringBuilder sb = new StringBuilder();
					String line = br.readLine();

					while (line != null)
					{
						sb.append(line);
						line = br.readLine();
					}
					String everything = sb.toString();

					noonce = Integer.parseInt(everything);
				}
				catch (IOException e)
				{
				}
				finally
				{
					try
					{
						br.close();
					}
					catch (IOException e)
					{
					}
				}

			}
			catch (FileNotFoundException e)
			{

			}
		}

		if (increase)
		{

			Writer writer = null;

			try
			{
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getUserDataDirectory() + "noonce.txt"), "utf-8"));
				writer.write(Integer.toString(noonce + 1));
			}
			catch (IOException ex)
			{
				// report
			}
			finally
			{
				try
				{
					writer.close();
				}
				catch (Exception ex)
				{
					/*ignore*/
				}
			}
			MainWindow.textFieldNoonce.setText(Integer.toString(noonce + 1));
		}
		else
		{
			MainWindow.textFieldNoonce.setText(Integer.toString(noonce));
		}

		return noonce;
	}
}
