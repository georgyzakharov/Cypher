package edu.sunypoly.cypher.backend.service;

//Date: 2 Nov. 2018

import java.io.*;
import java.util.Random;

public class DockerManager {

	private File gccDockerfile;
	private File openjdkDockerfile;
	private File pythonDockerfile;

	public DockerManager() {
		// "/home/$USER/Cypher/[gcc, openjdk, python]/Dockerfile"
		try {
			gccDockerfile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Cypher"
										+ System.getProperty("file.separator") + "gcc" + System.getProperty("file.separator")
										+ "Dockerfile");
			openjdkDockerfile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Cypher"
										+ System.getProperty("file.separator") + "openjdk" + System.getProperty("file.separator")
										+ "Dockerfile");
			pythonDockerfile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "Cypher"
										+ System.getProperty("file.separator") + "python" + System.getProperty("file.separator")
										+ "Dockerfile");
			if (!gccDockerfile.exists() || !openjdkDockerfile.exists() || !pythonDockerfile.exists()) {
				gccDockerfile.getParentFile().mkdirs();
				gccDockerfile.createNewFile();

				openjdkDockerfile.getParentFile().mkdirs();
				openjdkDockerfile.createNewFile();

				pythonDockerfile.getParentFile().mkdirs();
				pythonDockerfile.createNewFile();
			}
		}

		catch (SecurityException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
	}

/***************************************************************************************************/
/*
	File handling & utility methods
*/
/***************************************************************************************************/

	//Appends a string 's' to file 'f'
	public boolean write(File f, String s) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(f.getCanonicalFile(), true));

			writer.write(s, 0, s.length());

			writer.close();
			return true;
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//Writes a string 's' to file 'f', overwriting the file
	//with the length of the string if "true" is passed as the
	//third argument. If false is passed instead, the write(file, string)
	//method is called.
	public boolean write(File f, String s, boolean overwrite) {
		try {
			if (overwrite) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(f.getCanonicalFile()));

				writer.write(s, 0, s.length());

				writer.close();
				return true;
			}
			else {
				return write(f, s);
			}
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//BE CAREFUL!! This method will recursively delete all files
	//within the specified path, including directories!
	//To delete "/home/$USER/Cypher/[gcc,openjdk,python]/Dockerfile",
	//call "delete()" like so:
	//		File f = new File("/home/$USER/Cypher/[gcc,openjdk,python]/Dockerfile")
	//		delete("/home/$USER/Cypher")
	public boolean delete(File f) {
		try {
			if (f.getCanonicalFile().exists()) {
				if (f.isDirectory()) {
					for (File f_dir : f.listFiles()) {
						if (f_dir.isDirectory()) {
							delete(f_dir);
						}
						else {
							f_dir.delete();
						}
					}
				}
				f.delete();
				return true;
			}
			else {
				System.err.println("Error: File '" + f.getCanonicalPath() + "' does not exist");
			}
		}

		catch (SecurityException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//Remove old Dockerfiles if they exist (they shouldn't)
	public boolean init() {
		try {
			for (File f : gccDockerfile.getParentFile().getCanonicalFile().listFiles()) {
				if (f.exists()) {
					f.delete();
				}
			}
			gccDockerfile.createNewFile();

			for (File f : openjdkDockerfile.getParentFile().getCanonicalFile().listFiles()) {
				if (f.exists()) {
					f.delete();
				}
			}
			openjdkDockerfile.createNewFile();

			for (File f : pythonDockerfile.getParentFile().getCanonicalFile().listFiles()) {
				if (f.exists()) {
					f.delete();
				}
			}
			pythonDockerfile.createNewFile();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
			return false;
		}
		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
			return false;
		}

		return true;
	}

/***************************************************************************************************/
/*
	Generic Docker-related methods
*/
/***************************************************************************************************/

	//Checks if Docker is running
	public static boolean testDockerDaemon() {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			BufferedReader stdErr;
			String errorMessage = new String();
			String s = null;

			pb.command("docker", "version");

			Process p = pb.start();

			stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			while ((s = stdErr.readLine()) != null) {
				errorMessage = errorMessage + s;
			}

			if ((errorMessage != null) && (!errorMessage.isEmpty())) {
				stdErr.close();
				p.waitFor();
				p.destroy();
				System.err.println("Error: " + errorMessage);
			}
			else {
				if (stdErr != null) {
					stdErr.close();
				}
				p.waitFor();
				p.destroy();
				return true;
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//Sets up the group and user "appuser" for
	//a Docker container by writing the Linux commands
	//"useradd" and "groupadd" to the Dockerfile.
	//A randomly generated alphanumeric password is
	//used for "appuser", to limit privileged command
	//execution within the container (compiler and
	//source code executables are not privileged
	//and thus alone will never require a password
	//to execute).
	public boolean prepDockerfile(File f) {
		Random PRNG = new Random(System.currentTimeMillis());
		final int len = 16; //Password length
		int i;
		String password = new String();
		for (i = 0; i < len; i++) {
			//122 in ASCII is 'z'
			int n = PRNG.nextInt(123);
			
			//ASCII:
			//	91 = '[' , 92 = '\' , 93 = ']' , 94 = '^' ,
			// 95 = '_' , 96 = '`' ...all invalid password characters
			while ((n > 90) && (n < 97)) {
				n = PRNG.nextInt(123);
			}
			//65 in ASCII is 'A', so A-z
			if (n >= 65) {
				password = password + Character.toString((char)n);
			}
			else {
				password = password + Character.toString(Integer.toString(n).charAt(0));
			}
		}
		if (write(f, new String("RUN groupadd -g 999 anonymous "
					+ "&& useradd -r -m -u 999 -g anonymous -p "
					+ password + " anonymous\n"
					+ "USER anonymous"))) {
		 	return true;
		}

		return false;
	}

/***************************************************************************************************/
/*
	Methods for managing Docker images
*/
/***************************************************************************************************/

	//Checks if Docker image exists
	public static boolean checkDockerImage(String imageTag) {
		try {
			//List all Docker images and search images
			//for matching image tag
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("docker", "images");
			Process p = pb.start();

			BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String results = new String();
			String s = null;

			while ((s = stdIn.readLine()) != null) {
				results = results + s;
			}

			if (results.contains(imageTag)) {
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				return true;
			}
			else {
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		} 
	
		return false;
	}

	//Builds a Docker image
	public boolean buildDockerImage(File f, String imageTag) {
		try {
			if (f.getParentFile().isDirectory()) {
				ProcessBuilder pb = new ProcessBuilder();
				pb.directory(f.getParentFile().getCanonicalFile());
				pb.command("docker", "build", "-t", imageTag, ".");
				Process p = pb.start();
				
				BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String errorMessage = new String();
				String s = null;

				while ((s = stdErr.readLine()) != null) {
					errorMessage = errorMessage + s;
				}
		
				if ((errorMessage != null) && (!errorMessage.isEmpty())) {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					System.err.println("Error: " + errorMessage);
				}
				else {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					return true;
				}
			}
			else {
				System.err.println("Error: File '" + f.getParentFile().getCanonicalPath() + "' is not a directory");
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		return false;
	}

	//Removes Docker image if it exists, stopping and removing
	//Docker containers with the same name along the way
	public boolean removeDockerImage(String imageTag) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			BufferedReader stdErr;
			String errorMessage = new String();
			String s = null;

			if (checkDockerContainer(imageTag)) {
				//Stop any running containers with the same
				//name as the image tag
				if (stopDockerContainer(imageTag)) {
					System.out.println("Stopped Docker container '" + imageTag + "'");
				}

				//Remove any stopped containers with the same
				//name as the image tag
				if (removeDockerContainer(imageTag)) {
					System.out.println("Removed Docker container '" + imageTag + "'");
				}
			}

			if (checkDockerImage(imageTag)) {
				//Remove any Docker images with the same
				//name as the image tag
				pb.command("docker", "rmi", imageTag);
				
				System.out.println("Removing Docker image '" + imageTag + "'...");

				Process p = pb.start();

				stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

				errorMessage = null;
				s = null;

				while ((s = stdErr.readLine()) != null) {
					errorMessage = errorMessage + s;
				}

				if ((errorMessage != null) && (!errorMessage.isEmpty())) {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					System.err.println("Error: " + errorMessage);
				}
				else {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					System.out.println("Removed Docker image '" + imageTag + "'");
					return true;
				}
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//Pulls a Docker image from the Docker Hub.
	//NOTE: if the host system is not connected to
	//the Internet, then this method will hang
	//while waiting for the process to complete.
	public static boolean pullDockerImage(String imageTag) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("docker", "pull", imageTag);
			
			Process p = pb.start();
			
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			String errorMessage = new String();
			String input = new String();
			String s = null;
			
			while ((s = stdErr.readLine()) != null) {
				errorMessage = errorMessage + s;
			}
			
			while ((s = stdIn.readLine()) != null) {
				input = input + s;
			}
			
			if ((errorMessage != null) && (!errorMessage.isEmpty())) {
				if (stdErr != null) {
					stdErr.close();
				}
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				System.err.println("Error: " + errorMessage);
			}
			else if ((input != null) && (!input.isEmpty())) {
				if (stdErr != null) {
					stdErr.close();
				}
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				return true;
			}
			else {
				if (stdErr != null) {
					stdErr.close();
				}
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				System.err.println("Error: Docker daemon failed to write to STDOUT or STDERR streams");
			}
		}
		
		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

/***************************************************************************************************/
/*
	Methods for managing Docker containers
*/
/***************************************************************************************************/

	//Checks if Docker container exists
	public static boolean checkDockerContainer(String imageTag) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			pb.command("docker", "container", "ps", "-a");
			Process p = pb.start();

			BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String results = new String();
			String s = null;

			while ((s = stdIn.readLine()) != null) {
				results = results + s;
			}

			if (results.contains(imageTag)) {
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				return true;
			}
			else {
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//Runs a Docker container
	public boolean startDockerContainer(String imageTag) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			//"-d" switch means to run the container detached from STDIN/STDOUT, basically
			//running in the background. "--name=" assigns a name to the container, and the
			//Linux command "tail -f /dev/null" simply keeps the container up and running
			//rather than terminating once it is up, because by design containers terminate
			//when the root process in the container exits. Thus, "tail -f /dev/null" will run
			//forever and keep the container up and running until it is stopped.
			pb.command("docker", "run", "-d", new String("--name=" + imageTag), imageTag, "tail", "-f", "/dev/null");

			Process p = pb.start();

			BufferedReader stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String errorMessage = new String();
			String processInput = new String();
			String s = null;

			while ((s = stdErr.readLine()) != null) {
				errorMessage = errorMessage + s;
			} 

			while ((s = stdIn.readLine()) != null) {
				processInput = processInput + s;
			}

			if ((errorMessage != null) && (!errorMessage.isEmpty())) {
				if (stdErr != null) {
					stdErr.close();
				}
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				System.err.println("Error: " + errorMessage);
			}

			else if ((processInput != null) && (!processInput.isEmpty())) {
				if (stdErr != null) {
					stdErr.close();
				}
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				return true;
			}

			else {
				if (stdErr != null) {
					stdErr.close();
				}
				if (stdIn != null) {
					stdIn.close();
				}
				p.waitFor();
				p.destroy();
				System.err.println("Error: Docker container process failed to write to STDERR or STDOUT streams");
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		return false;
	}

	//Stops a Docker container
	public boolean stopDockerContainer(String imageTag) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			BufferedReader stdErr;
			String errorMessage = new String();
			String s = null;

			if (checkDockerContainer(imageTag)) {
				//Stop any running containers with the same
				//name as the image tag
				pb.command("docker", "container", "stop", imageTag);
				
				Process p = pb.start();

				stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

				while ((s = stdErr.readLine()) != null) {
					errorMessage = errorMessage + s;
				}

				if ((errorMessage != null) && (!errorMessage.isEmpty())) {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					System.err.println("Error: " + errorMessage);
				}
				else {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					return true;
				}
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}

	//Removes a Docker container
	public boolean removeDockerContainer(String imageTag) {
		try {
			ProcessBuilder pb = new ProcessBuilder();
			BufferedReader stdErr;
			String errorMessage = new String();
			String s = null;

			if (checkDockerContainer(imageTag)) {
				//Remove any paused or stopped Docker containers
				//with the provided name ("imageTag")
				pb.command("docker", "container", "rm", imageTag);
				
//				System.out.println("Removing Docker container '" + imageTag + "'...");

				Process p = pb.start();

				stdErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));

				while ((s = stdErr.readLine()) != null) {
					errorMessage = errorMessage + s;
				}

				if ((errorMessage != null) && (!errorMessage.isEmpty())) {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					System.err.println("Error: " + errorMessage);
				}
				else {
					if (stdErr != null) {
						stdErr.close();
					}
					p.waitFor();
					p.destroy();
					return true;
				}
			}
		}

		catch (InterruptedException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		
		return false;
	}


/***************************************************************************************************/
/*
	Methods for managing multiple Docker containers for the Cypher sandbox
*/
/***************************************************************************************************/


	//Starts up the containers "gcc-cypher", "openjdk-cypher"
	//and "python-cypher" from scratch. Old containers (if there
	//are any) are stopped and removed, old images are removed,
	//and entirely new images are built from which new containers
	//are started.
	public boolean startContainers(String ...conts) {
		boolean gcc = false;
		boolean openjdk = false;
		boolean python = false;

		System.out.println();

		for (String container : conts) {
			if (!removeDockerImage(container)) {
				System.out.println("Found no images or containers tagged as '" + container + "'");
			}
		}

		try {
			if (init()) {
				for (String container : conts) {
					//GCC Docker container
					System.out.println();
					if (container.contentEquals("gcc-cypher")) {
						if (write(gccDockerfile, "FROM gcc\n")) {
							if (prepDockerfile(gccDockerfile)) {
								System.out.println("Building Docker image '" + container + "'...");
								if (buildDockerImage(gccDockerfile, container)) {
									System.out.println("Built Docker image '" + container + "'");
									System.out.println("Starting Docker container '" + container + "'...");
									if (startDockerContainer(container)) {
										gcc = true;
										System.out.println("Started Docker container '" + container + "'...");
										delete(new File(new String(System.getProperty("user.home")
													+ System.getProperty("file.separator")
													+ "Cypher" + System.getProperty("file.separator")
													+ "gcc")));
									}
									else {
										System.err.println("!-->Error: Failed to run Docker container '" + container + "'");
									}
								}
								else {
									System.err.println("!-->Error: Failed to build Docker image '" + container + "'"
														+ " from Dockerfile " + gccDockerfile.getCanonicalPath());
								}
							}
							else {
								System.err.println("!-->Error: Failed to append to Dockerfile "
													+ gccDockerfile.getCanonicalPath());
							}
						}
						else {
							System.err.println("!-->Error: Failed to write to Dockerfile "
													+ gccDockerfile.getCanonicalPath());
						}
					}
					else if (container.contentEquals("openjdk-cypher")) {
						//OpenJDK Docker container
						if (write(openjdkDockerfile, "FROM openjdk\n")) {
							if (prepDockerfile(openjdkDockerfile)) {
								System.out.println("Building Docker image '" + container + "'...");
								if (buildDockerImage(openjdkDockerfile, container)) {
									System.out.println("Built Docker image '" + container + "'");
									System.out.println("Starting Docker container '" + container + "'...");
									if (startDockerContainer(container)) {
										openjdk = true;
										System.out.println("Started Docker container '" + container + "'...");
										delete(new File(new String(System.getProperty("user.home")
													+ System.getProperty("file.separator")
													+ "Cypher" + System.getProperty("file.separator")
													+ "openjdk")));
									}
									else {
										System.err.println("!-->Error: Failed to run Docker container '" + container + "'");
									}
								}
								else {
									System.err.println("!-->Error: Failed to build Docker image '" + container + "'"
														+ " from Dockerfile " + openjdkDockerfile.getCanonicalPath());
								}
							}
							else {
								System.err.println("!-->Error: Failed to append to Dockerfile "
													+ openjdkDockerfile.getCanonicalPath());
							}
						}
						else {
							System.err.println("!-->Error: Failed to write to Dockerfile "
													+ openjdkDockerfile.getCanonicalPath());
						}
					}
					else if (container.contentEquals("python-cypher")) {
						//Python Docker container
						if (write(pythonDockerfile, "FROM python\n")) {
							if (prepDockerfile(pythonDockerfile)) {
								System.out.println("Building Docker image '" + container + "'...");
								if (buildDockerImage(pythonDockerfile, container)) {
									System.out.println("Built Docker image '" + container + "'");
									System.out.println("Starting Docker container '" + container + "'...");
									if (startDockerContainer(container)) {
										python = true;
										System.out.println("Started Docker container '" + container + "'...");
										delete(new File(new String(System.getProperty("user.home")
															+ System.getProperty("file.separator")
															+ "Cypher" + System.getProperty("file.separator")
															+ "python")));
									}
									else {
										System.err.println("!-->Error: Failed to run Docker container '" + container + "'");
									}
								}
								else {
									System.err.println("!-->Error: Failed to build Docker image '" + container + "'"
														+ " from Dockerfile " + pythonDockerfile.getCanonicalPath());
								}
							}
							else {
								System.err.println("!-->Error: Failed to append to Dockerfile "
													+ pythonDockerfile.getCanonicalPath());
							}
						}
						else {
							System.err.println("!-->Error: Failed to write to Dockerfile "
													+ pythonDockerfile.getCanonicalPath());
						}
					}
					else {
						System.err.println("!-->Error: Failed to start '" + container + "' because the container name "
												+ "does not match any of the following permissible container names: "
												+ "'gcc-cypher', 'openjdk-cypher', 'python-cypher'");
					}
				}	
			}
			else {
				System.err.println("!-->Error: Failed to delete files: " + gccDockerfile.getCanonicalPath()
									+ ", " + openjdkDockerfile.getCanonicalPath() 
									+ ", " + pythonDockerfile.getCanonicalPath());
			}

			//Deleting directory "/home/$USER/Cypher"
			if (gcc && openjdk && python) {
				delete(new File(new String(System.getProperty("user.home") + System.getProperty("file.separator")
												+ "Cypher")));
			}
		}

		catch (IOException e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		return (gcc && openjdk && python);
		//return (gcc || openjdk || python);
	}

	//Stops and removes Docker containers
	public boolean stopContainers(String ...conts) {
		boolean gcc = false;
		boolean openjdk = false;
		boolean python = false;
		
		try {
			for (String container : conts) {
				if (checkDockerContainer(container)) {
					System.out.println("\nStopping Docker container '" + container + "'...");
					if (stopDockerContainer(container)) {
						System.out.println("Stopped Docker container '" + container + "'");
						System.out.println("Removing Docker container '" + container + "'...");
						if (removeDockerContainer(container)) {
							System.out.println("Removed Docker container '" + container + "'");
							if (container.contentEquals("gcc-cypher")) {
								gcc = true;
							}
							else if (container.contentEquals("openjdk-cypher")) {
								openjdk = true;
							}
							else if (container.contentEquals("python-cypher")) {
								python = true;
							}
							else {
								continue;
							}
						}
						else {
							System.err.println("!-->Error: Failed to remove container '" + container + "'");
						}
					}
					else {
						System.err.println("!-->Error: Failed to stop container '" + container + "'");	
					}
				}
				else {
					System.err.println("!-->Error: Found no container matching the name '" + container + "'");
				}
			}
		}
		
		catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
	
		return (gcc && openjdk && python);
		//return (gcc || openjdk || python);
	}

/***************************************************************************************************/
/*
	Starts up the Cypher sandbox
*/
/***************************************************************************************************/

	public static void main(String[] args) {
		DockerManager dock = new DockerManager();

		//Tests to see if Docker daemon is running
		if (DockerManager.testDockerDaemon()) {
			boolean gcc = false;
			boolean openjdk = false;
			boolean python = false;

			//Stops containers
			if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
				if (!dock.stopContainers("gcc-cypher", "openjdk-cypher", "python-cypher")) {
					System.err.println("Failed to stop all Docker containers...");
				}
				else {
					System.out.println();
				}
			}
			else {
				//Gets Docker image 'gcc' if not installed.
				//MAY HANG DURING DOWNLOAD!
				if (!DockerManager.checkDockerImage("gcc")) {
					System.out.println("Found no base image tagged as 'gcc'");
					System.out.println("Attempting to download image 'gcc' from Docker Hub. THIS MAY TAKE SOME TIME...");
					if (DockerManager.pullDockerImage("gcc")) {
						System.out.println("Downloaded image 'gcc' from Docker Hub.");
						gcc = true;
					}
					else {
						System.err.println("!-->Error: Failed to download image 'gcc' from Docker Hub");
					}
				}
				else {
					gcc = true;
				}

				//Gets Docker image 'openjdk' if not installed.
				//MAY HANG DURING DOWNLOAD!
				if (!DockerManager.checkDockerImage("openjdk")) {
					System.out.println("Found no base image tagged as 'openjdk'");
					System.out.println("Attempting to download image 'openjdk' from Docker Hub. THIS MAY TAKE SOME TIME...");
					if (DockerManager.pullDockerImage("openjdk")) {
						System.out.println("Downloaded image 'openjdk' from Docker Hub.");
						openjdk = true;
					}
					else {
						System.err.println("!-->Error: Failed to download image 'openjdk' from Docker Hub");
					}
				}
				else {
					openjdk = true;
				}

				//Gets Docker image 'python' if not installed.
				//MAY HANG DURING DOWNLOAD!
				if (!DockerManager.checkDockerImage("python")) {
					System.out.println("Found no base image tagged as 'python'");
					System.out.println("Attempting to pull download 'python' from Docker Hub. THIS MAY TAKE SOME TIME...");
					if (DockerManager.pullDockerImage("python")) {
						System.out.println("Downloaded image 'python' from Docker Hub.");
						python = true;
					}
					else {
						System.err.println("!-->Error: Failed to download image 'python' from Docker Hub");
					}
				}
				else {
					python = true;
				}


				if (gcc && openjdk && python) {
					if (dock.startContainers("gcc-cypher", "openjdk-cypher", "python-cypher")) {
						System.out.println("\nSuccessfully started all Docker containers!\n");
					}
					else {
						System.err.println("Error: Failed to start all Docker containers...");
					}
				}
				else {
					System.err.println("!-->Error: Failed to start Docker containers due to the following missing Docker images:");
					if (!gcc) System.err.println("    --->'gcc'");
					if (!openjdk) System.err.println("    --->'openjdk'");
					if (!python) System.err.println("    --->'python'");
				}
			}

		}
		else {
			System.err.println("\nError: Docker is not running. Terminated execution.\n");
		}

		return;
	}

}









