package edu.sunypoly.cypher.db;

import java.io.File;
import java.nio.file.Files;

public class Driver {
	public static void main(String[] args) throws AlreadyExistsException, NullInputException, InvalidDataException {
	Mis Manager = new Mis("jdbc:mysql://localhost/cypher_db?useSSL=false", "cypher", "cypher");
	
	Manager.Team.create("team1","password");
	System.out.println(Manager.Team.getId("team1"));
	}
}
