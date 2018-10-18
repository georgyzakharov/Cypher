package edu.sunypoly.cypher.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SolutionManager
{
    private static Connection SQLCON = null; 
    public SolutionManager(Connection INSQLCON)
    {
        SQLCON = INSQLCON;
    }

    public boolean create(String solutionName, int teamId, int problemId, String language, byte[] solution) throws AlreadyExistsException
    {
        boolean success = false;
        PreparedStatement nameStmt = null;
        PreparedStatement storageStmt = null;
        String nameQuery = "INSERT INTO solution(name, team_id, problem_id, language) VALUES (?,?,?,?);";
        String storageQuery = "INSERT INTO solution_storage(id, solution) VALUES (?,?);";
        if(getId(solutionName) == -1)
        {
            try
            {
                nameStmt = SQLCON.prepareStatement(nameQuery);
                nameStmt.setString(1, solutionName);
                nameStmt.setInt(2, teamId);
                nameStmt.setInt(3, problemId);
                nameStmt.setString(4, language);
                nameStmt.execute();

                storageStmt = SQLCON.prepareStatement(storageQuery);
                storageStmt.setInt(1, getId(solutionName));
                storageStmt.setBytes(2, solution);
                storageStmt.execute();
                

            } catch (SQLException e) 
            {
                System.err.println("SQLException: " + e.getMessage());
                System.err.println("SQLState: " + e.getSQLState());
                System.err.println("VendorError: " + e.getErrorCode());
            }
            if(getId(solutionName) != -1)
                success = true;
        }
        else
            throw new AlreadyExistsException(solutionName + "already exists!");
        return success;
    }
    public boolean update(int solutionId, String solutionName, int teamId, int problemId, String language, int score, byte[] solution) throws DoesNotExistException, AlreadyExistsException 
    {
        boolean success = false;
        PreparedStatement fkStmt = null;
        PreparedStatement stmt = null;
        String fk = "SET FOREIGN_KEY_CHECKS=?;";
        String nameQuery = "UPDATE solution SET name = ?, team_id = ?, problem_id = ?, language = ?, score = ? WHERE id = ?";
        String storageQuery = "UPDATE solution_storage SET solution = ? WHERE id = ?";
        if(getName(solutionId) != null && getId(solutionName) == -1)
        {
            try
            {
                fkStmt = SQLCON.prepareStatement(fk);
                fkStmt.setInt(1, 0);
                fkStmt.executeUpdate();

                stmt = SQLCON.prepareStatement(nameQuery);
                stmt.setString(1, solutionName);
                stmt.setInt(2, teamId);
                stmt.setInt(3, problemId);
                stmt.setString(4, language);
                stmt.setInt(5, score);
                stmt.setInt(6, solutionId);
                stmt.executeUpdate();

                stmt = SQLCON.prepareStatement(storageQuery);
                stmt.setBytes(1, solution);
                stmt.setInt(2, solutionId);
                stmt.executeUpdate();

                fkStmt = SQLCON.prepareStatement(fk);
                fkStmt.setInt(1, 1);
                fkStmt.executeUpdate();
            }
            catch (SQLException e) 
            {
                System.err.println("SQLException: " + e.getMessage());
                System.err.println("SQLState: " + e.getSQLState());
                System.err.println("VendorError: " + e.getErrorCode());
            }
            if(getId(solutionName) != -1)
                success = true;
        }
        else if(getName(problemId) == null)
            throw new DoesNotExistException(solutionId + " does not exist!");
        else
            throw new AlreadyExistsException(solutionId + " is already a team!");   

        return success;

    }
    public boolean delete(int solutionId)
    {
        boolean success = false;
        PreparedStatement stmt = null;
        String nameQuery = "DELETE FROM solution WHERE id = ?;";
        //String storageQuery = "DELETE FROM solution_storage WHERE id = ?;";
        if(getName(solutionId) != null)
        {
            try
            {
                /*
                stmt = SQLCON.prepareStatement(storageQuery);
                stmt.setInt(1, solutionId);
                stmt.executeUpdate();
                */

                stmt = SQLCON.prepareStatement(nameQuery);
                stmt.setInt(1, solutionId);
                stmt.executeUpdate();
            }
            catch (SQLException e)
            {
                /*
                System.err.println("SQLException: " + e.getMessage());
                System.err.println("SQLState: " + e.getSQLState());
                System.err.println("VendorError: " + e.getErrorCode());
                */
            }
            if(getName(solutionId) == null)
                success = true;

        }
        return success;
    }
    
    public boolean delete(String solutionName)
    {
        boolean success = false;
        PreparedStatement stmt = null;
        String nameQuery = "DELETE FROM solution WHERE id = ?;";
        //String storageQuery = "DELETE FROM solution_storage WHERE id = ?;";
        if(getId(solutionName) != -1)
        {
            try
            {
                /*
                stmt = SQLCON.prepareStatement(storageQuery);
                stmt.setInt(1, getId(solutionName));
                stmt.executeUpdate();
                */

                stmt = SQLCON.prepareStatement(nameQuery);
                stmt.setInt(1, getId(solutionName));
                stmt.executeUpdate();
            }
            catch (SQLException e)
            {
                
                System.err.println("SQLException: " + e.getMessage());
                System.err.println("SQLState: " + e.getSQLState());
                System.err.println("VendorError: " + e.getErrorCode());
                
            }
            
            if(getId(solutionName) == -1)
                success = true;
    
        }
        return success;
    }

    public byte[] getSolution(String solutionName)
    {
        byte[] solution = null;
        PreparedStatement stmt = null;
        String query = "SELECT solution FROM solution_storage WHERE name = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setString(1, solutionName);
            ResultSet result = stmt.executeQuery();
            result.next();
            solution = result.getBytes("solution");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return solution;
    
    }
    public byte[] getSolution(int solutionId)
    {
        byte[] solution = null;
        PreparedStatement stmt = null;
        String query = "SELECT solution FROM solution_storage WHERE id = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setInt(1, solutionId);
            ResultSet result = stmt.executeQuery();
            result.next();
            solution = result.getBytes("solution");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return solution;
    }
    public int getScore(String solutionName)
    {
        int score = -1;
        PreparedStatement stmt = null;
        String query = "SELECT score FROM solution WHERE name = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setString(1, solutionName);
            ResultSet result = stmt.executeQuery();
            result.next();
            score = result.getInt("score");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return score;
    }
    public int getScore(int solutionId)
    {
        int score = -1;
        PreparedStatement stmt = null;
        String query = "SELECT score FROM solution WHERE id = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setInt(1, solutionId);
            ResultSet result = stmt.executeQuery();
            result.next();
            score = result.getInt("score");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return score;
    }
    public String getLanguage(String solutionName)
    {
        String language = null;
        PreparedStatement stmt = null;
        String query = "SELECT language FROM solution WHERE name = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setString(1, solutionName);
            ResultSet result = stmt.executeQuery();
            result.next();
            language = result.getString("language");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return language;
    }
    public String getLanguage(int solutionId)
    {
        String language = null;
        PreparedStatement stmt = null;
        String query ="SELECT language FROM solution WHERE id = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setInt(1, solutionId);
            ResultSet result = stmt.executeQuery();
            result.next();
            language = result.getString("language");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return language;
    }
    public int getProblemId(int solutionId)
    {
        int problemId = -1;
        PreparedStatement stmt = null;
        String query = "SELECT problem_id FROM solution WHERE id = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setInt(1, solutionId);
            ResultSet result = stmt.executeQuery();
            result.next();
            problemId = result.getInt("problem_id");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return problemId;
    }
    public int getProblemId(String solutionName)
    {
        int problemId = -1;
        PreparedStatement stmt = null;
        String query = "SELECT problem_id FROM solution WHERE name = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setString(1, solutionName);
            ResultSet result = stmt.executeQuery();
            result.next();
            problemId = result.getInt("problem_id");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return problemId;
    }
    public int getTeamId(String solutionName)
    {
        int teamId = -1;
        PreparedStatement stmt = null;
        String query = "SELECT team_id FROM solution WHERE name = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setString(1, solutionName);
            ResultSet result = stmt.executeQuery();
            result.next();
            teamId = result.getInt("team_id");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return teamId;
    }
    public int getTeamId(int solutionId)
    {
        int teamId = -1;
        PreparedStatement stmt = null;
        String query = "SELECT team_id FROM solution WHERE id = ?;";
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setInt(1, solutionId);
            ResultSet result = stmt.executeQuery();
            result.next();
            teamId = result.getInt("team_id");
        }
        catch (SQLException e) 
        {
            System.err.println("SQLException: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("VendorError: " + e.getErrorCode());
        }
        return teamId;
    }

    public String getName(int solutionId)
    {
        String solutionName = null;
        PreparedStatement stmt = null;
        String query = "SELECT name FROM solution WHERE id = ?;";    
        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setInt(1, solutionId);
            ResultSet result = stmt.executeQuery();
            result.next();
            solutionName = result.getString("name");
        }
        catch (SQLException e) 
        {

        }
        return solutionName;
    }

    public int getId(String solutionName)
    {
        int solutionId = -1;
        PreparedStatement stmt = null;
        String query = "SELECT id FROM solution WHERE name = ?;";

        try
        {
            stmt = SQLCON.prepareStatement(query);
            stmt.setString(1, solutionName);
            ResultSet result = stmt.executeQuery();
            result.next();
            solutionId = result.getInt("id");
        }
        catch (SQLException e) 
        {

        }
        return solutionId;
    }

}