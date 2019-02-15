
package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;


public class Server {

    ServerSocket myServerSocket;
    boolean ServerOn = true;


    public Server() 
    { 
        try 
        { 
            myServerSocket = new ServerSocket(5000); 
            System.out.println("Connection established,Waiting for requests");
            
        } 
        catch(IOException ioe) 
        { 
            System.out.println("Could not create server socket on port 5000. Quitting."); 
            System.exit(-1); 
        } 
       





      


        // Successfully created Server Socket. Now wait for connections. 
        while(ServerOn) 
        {                        
            try 
            { 
                // Accept incoming connections. 
                Socket clientSocket = myServerSocket.accept(); 

                // Starting a Service thread 

                ClientServiceThread cliThread = new ClientServiceThread(clientSocket);
                cliThread.start(); 

            } 
            catch(IOException ioe) 
            { 
                System.out.println(ioe.getMessage()); 
            } 

        }



    } 
    

    public static void main (String[] args) 
    { 
        new Server();        
    } 


    class ClientServiceThread extends Thread 
    { 
        Socket myClientSocket;
        PrintStream p;
        
      
        ClientServiceThread(Socket s) 
        { 
            myClientSocket = s; 
            
        } 

        public void run() 
        {            

            // Printing out details of this connection 
            System.out.println("\nAccepted Client Address - " + myClientSocket.getInetAddress().getHostName()); 

            try 
            {                                
                // reading incoming stream 
                ObjectOutputStream out = new ObjectOutputStream(myClientSocket.getOutputStream());
                ObjectInputStream oo=new ObjectInputStream(myClientSocket.getInputStream());
                
                
                String a[] = (String[])oo.readObject(); //reading input object
                System.out.println("Client Says :" + a[0]);

                if (a[0].equals("register"))
                {
                    
                    try{
                        System.out.println("Registering");
                        String register="INSERT INTO `customer_info` (`Customer_FirstName`, `Customer_LastName`, `Cabin_Number`, "
                                + "`Email`, `Password`, `Contact`) "
                                + "VALUES ('"+a[1]+"','"+a[2]+"','"+a[3]+"','"+a[4]+"','"+a[5]+"','"+a[6]+"');";
                        
                        String [] message={Register(register)};
                        out.writeObject(message);
                    }
                   
                    catch(Exception e)
                    {
                       e.printStackTrace();
                    }

                }
                else if(a[0].equals("login"))
                {
                    System.out.println("Checking credentials");
                    for (String b:a){
                        System.out.println(b);
                    }
                    String Status=Login(a);
                    p= new PrintStream(myClientSocket.getOutputStream(),true);
                    System.out.print(Status);
                    p.print(Status+"\n");

                }
                else if (a[0].equals("view"))
                    {   
                        out.writeObject(View("customer_info",8));
                    }
                else if (a[0].equals("view_booking"))
                    {   
                        System.out.print("sending booking");
                        out.writeObject(View("bookings where Booking_Status=1",7));
                    }
                else if (a[0].equals("bookingQuery"))
                    { 
                        System.out.print("sending query");
                        out.writeObject(View("bookings where Booking_Status=1 and Ex_ID = "+ a[1],7));
                    }
                else if (a[0].equals("CustomerQuery"))
                    { 
                        System.out.print("sending query");
                        out.writeObject(View("customer_info where Email like '"+ a[1]+"%'",8));
                    }
                
                else if (a[0].equals("cancel"))
                    {
                        System.out.println("updating");
                        cancel(a[1],a[2]);
                       
                    }
                else if (a[0].equals("cancelWaiting"))
                    {
                        System.out.println("Canceling waiting");
                         Connection conn=getConnection();
                        PreparedStatement st=conn.prepareStatement("select Customer_ID from customer_info where Email = '"+a[1]+"'"); 
                        ResultSet rs=st.executeQuery();
                        rs.next();
                        String ID=rs.getString(1);
                        String query1="SELECT Excursion_ID FROM excursion WHERE Ex_name = '"+a[2]+"'";
                        ResultSet res=st.executeQuery(query1);
                        res.next();
                        String exID=res.getString(1);
                        System.out.println(exID);
                        PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET Waiting_status = 0 WHERE Waiting_status = 1 and C_ID = ? and Ex_Id=? ;");  
                        stt.setString(1,ID);
                        stt.setString(2,exID);
                        stt.executeUpdate();
                        
                    }
                else if (a[0].equals("updateWaiting"))
                    {
                        System.out.println("updating waiting");
                         Connection conn=getConnection();
                        PreparedStatement st=conn.prepareStatement("select Customer_ID from customer_info where Email = '"+a[1]+"'"); 
                        ResultSet rs=st.executeQuery();
                        rs.next();
                        String ID=rs.getString(1);
                        String query1="SELECT Excursion_ID FROM excursion WHERE Ex_name = '"+a[2]+"'";
                        ResultSet res=st.executeQuery(query1);
                        res.next();
                        String exID=res.getString(1);
                        System.out.println(exID);
                        PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET No_of_seats = ? WHERE Waiting_status = 1 and C_ID = ? and Ex_Id=? ;");  
                        stt.setString(1,a[3]);
                        stt.setString(2,ID);
                        stt.setString(3,exID);
                        stt.executeUpdate();
                        
                    }
                else if (a[0].equals("return_combo"))
                    {
                        System.out.println("returning port number");
                        Connection conn=getConnection();
                        PreparedStatement stmt=conn.prepareStatement("select Ex_name from excursion where Port_ID = ?");  
                        stmt.setString(1,(String)a[1]);
                        ResultSet rs=stmt.executeQuery();
                        PreparedStatement st=conn.prepareStatement("select Ex_name from excursion where Port_ID = ?");  
                        st.setString(1,(String)a[1]);
                        ResultSet rslt=st.executeQuery();
                        int count=0;
                        while (rs.next()) {
                            count++;
                        }
                        String b[]=new String[count];
                        int i=0;
                        while(rslt.next())
                            {

                                    b[i] = rslt.getString(1);

                             i++;      
                             }
                        
                       
                        out.writeObject(b);
                        out.flush();
                        
                    }
                else if (a[0].equals("MaxBooking"))
                    {   
                        
                            Connection conn=getConnection();
                            Statement ste1=conn.createStatement();
                            ResultSet rs2=ste1.executeQuery("Select Customer_ID from customer_info where Email = '"+a[5]+"'");
                            rs2.next();
                            String CID=rs2.getString(1);
                            Statement st=conn.createStatement();
                            ResultSet rs=st.executeQuery("Select Excursion_ID from excursion where Ex_name = '"+a[6]+"'");
                            rs.next();
                            int ExID=Integer.parseInt(rs.getString(1));
                            PreparedStatement stmt1=conn.prepareStatement("select No_of_Seats from bookings where Ex_ID =? and Booking_status=1 and C_ID=?");
                            stmt1.setString(1,Integer.toString(ExID));
                            stmt1.setString(2,CID);
                            ResultSet rslt1=stmt1.executeQuery();
                            String [] message1={"Exists"};
                            if (rslt1.next())//checking if customer already booked                      
                                out.writeObject(message1);
                            else{
                               
                            System.out.print("Bookings Excursion");


                            PreparedStatement stmt=conn.prepareStatement("select No_of_Seats from bookings where Ex_ID =? and Booking_status=1");
                            stmt.setString(1,Integer.toString(ExID));
                            
                            ResultSet rslt=stmt.executeQuery();
                            int i=0;
                            while (rslt.next())
                                {
                                    i+=Integer.parseInt(rslt.getString(1));
                                }
                            System.out.println(i);
                            int available=32-i;
                            int m=i+Integer.parseInt(a[1]);//sum of existing and request seat
                            System.out.println(a[1]+","+a[6]);
                            if (m>32)
                                {   
                                    a[1]=Integer.toString(available);
                                    System.out.println(a[2]+","+a[6]);
                                    System.out.println("Booking Succesful");
                                    String message[]={"Booking Succesful","Booking Extended",Integer.toString(available)};
                                    out.writeObject(message);      
                                }
                            else{
                                String message[]={"Booking Succesful",Integer.toString(available)};
                                out.writeObject(message);      
                            }
                            }
                    }
                else if (a[0].equals("Booking"))
                    {   
                        System.out.print("adding to booking");
                        if (!a[1].equals("0")){
                            insert(a);
                        }
                    }
                else if (a[0].equals("Waiting"))
                    {   
                        System.out.print("Sending waitinglist");
                        out.writeObject(View("bookings where Waiting_status = 1",7));
                    }
                else if (a[0].equals("return port"))
                    {//sending port numbers to client
                        System.out.print("returning port number");
                        Connection conn=getConnection();
                        PreparedStatement stmt=conn.prepareStatement("select DISTINCT Port_ID from excursion");  
                        ResultSet rs=stmt.executeQuery();
                        PreparedStatement st=conn.prepareStatement("select DISTINCT Port_ID from excursion");  
                        ResultSet rslt=st.executeQuery();
                        int count=0;
                        while (rs.next()) 
                            {
                                count++;
                            }
                        String b[]=new String[count];
                        int i=0;
                        while(rslt.next())
                            {
                                    b[i] = rslt.getString(1);
                             i++;      
                            }
                        
                       
                        out.writeObject(b);
                        out.flush();
                    }
                else if (a[0].equals("Waiting_Booking"))
                    {   
                        System.out.print("Moving to booking");
                       
                        String [] b=new String[1];
                        if (availableSeats(a[7])<Integer.parseInt(a[2])){
                            b[0]="no seats";
                            System.out.print("Not enough seats");

                        }
                        else
                        {   System.out.print("Moving to booking");
                            b[0]="success";
                            Connection conn=getConnection();
                            Update(a,"Waiting_status",5,"C_ID",6);
                            Update(a,"Booking_status",4,"C_ID",6);
                            PreparedStatement stmt=conn.prepareStatement("select * from bookings where Booking_status=1 and C_ID=? and Ex_ID=?"); 
                            stmt.setString(1,a[6]);
                            stmt.setString(2,a[7]);
                            ResultSet rs=stmt.executeQuery();
                            
                            rs.next();
                            int previous=rs.getInt("No_of_Seats");
                            String b_ID=rs.getString("Booking_Id");
                           
                    
                            PreparedStatement stet=conn.prepareStatement("UPDATE bookings SET booking_status = 0 WHERE Booking_Id = ? ;");  
                            stet.setString(1,b_ID);
                            stet.executeUpdate();
                    
                            if (rs.next()){
                                 try {
             
                                PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET No_of_seats = ? WHERE C_ID = ? and Ex_ID=? and Booking_Id = ? ;"); 
                                stt.setString(1,String.valueOf(Integer.parseInt(a[2])+previous));
                                stt.setString(2,(String)a[6]);
                                stt.setString(3,(String)a[7]);
                                stt.setString(4,(String)a[1]);
                                stt.executeUpdate();
                                }
                                catch(Exception e){
                                e.printStackTrace();}
                            }
                            else{
                                  try {
             
                                PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET booking_status=1 WHERE C_ID = ? and Ex_ID=? and Booking_Id = ? ;"); 
                                
                                stt.setString(1,(String)a[6]);
                                stt.setString(2,(String)a[7]);
                                stt.setString(3,(String)a[1]);
                                stt.executeUpdate();
                                }
                                catch(Exception e){
                                e.printStackTrace();}
                            
                            
                                
                            }
                                    
                            
                        }
                        out.writeObject(b);
                        out.flush();
                    }
                else if (a[0].equals("AddWaiting"))
                    {   
                        System.out.print("Adding to waitlist");
                        Connection conn=getConnection();
                        PreparedStatement stmt=conn.prepareStatement("Select Customer_ID from customer_info where Email=?");
                        stmt.setString(1, a[5]);
                        ResultSet rs=stmt.executeQuery();
                        rs.next();
                        String CID=rs.getString(1);
                        Statement st=conn.createStatement();
                        String query1="SELECT Excursion_ID FROM excursion WHERE Ex_name = '"+a[6]+"'";
                        ResultSet rslt=st.executeQuery(query1);
                        rslt.next();
                        String ExID=rslt.getString(1);
                        PreparedStatement stt=conn.prepareStatement("Select * from bookings where C_ID=? and Ex_ID=? and Waiting_Status=1");
                        stt.setString(1, CID);
                        stt.setString(2, ExID);
                        ResultSet res=stt.executeQuery();
                    
                    
                    
                        if (res.next()){
                            PreparedStatement stet=conn.prepareStatement("Update bookings set No_of_Seats=? where C_ID=? and Ex_ID=? and Waiting_Status=1");
                            stet.setString(1, a[1]);
                            stet.setString(2, CID);
                            stet.setString(3, ExID);
                            stet.executeUpdate();
                        }
                        else{
                        insert(a);
                        }
                    }
                else if(a[0].equals("customerWaiting")){
                    System.out.println("Customer Waiting LIst");
                    out.writeObject(customerWaitingView(a[1]));
                }
                else if (a[0].equals("Check Booking"))
                    {
                         try {
                    Connection conn=getConnection();
                    PreparedStatement stmt=conn.prepareStatement("SELECT excursion.Port_ID,excursion.Ex_name, bookings.No_of_Seats, bookings.date ,bookings.Booking_Id\n" +
                                                                "FROM bookings\n" +
                                                                "INNER JOIN customer_info ON customer_info.Customer_ID = bookings.C_ID\n" +
                                                                "INNER JOIN excursion ON bookings.Ex_Id=excursion.Excursion_ID\n" +
                                                                "where Booking_status=1 and customer_info.Email=?");
                    stmt.setString(1,a[1]);
                    ResultSet rslt=stmt.executeQuery();

                    int count=0;
                    while (rslt.next()) {
                        count++;
                    }
                    rslt.beforeFirst();
                    int i=0;                   
                    String[][] arr = new String[count][5];
                    while(rslt.next())
                    {
                        for(int j=0;j<5;j++)
                        {
                            arr[i][j] = rslt.getString(j+1);
                            System.out.print(arr[i][j]);   
                        }
                        i++;                    
                    }
                    out.writeObject(arr);
                    
                    conn.close();
                    
		  } catch (Exception e) {
		  e.printStackTrace();
		  }
                   out.flush();
                        
                    }
                else if (a[0].equals("cancelBooking"))
                    {   
                        System.out.print("Canceling booking");
                        try {
                                Connection conn=getConnection();
                                PreparedStatement stmt=conn.prepareStatement("UPDATE bookings SET Booking_status = 0 WHERE Booking_status = 1 and C_ID = ? and Ex_ID=?;");  
                                stmt.setString(1,a[6]);
                                stmt.setString(2,a[7]);
                                stmt.executeUpdate();
                                PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET Waiting_status = 0 WHERE Waiting_status = 1 and C_ID = ? and Ex_ID=?;");  
                                stt.setString(1,a[6]);
                                stt.setString(2,a[7]);
                                stt.executeUpdate();

                                }
       catch(Exception e){
           e.printStackTrace();
           }
                    }
                else if (a[0].equals("update"))
                    {//updating bookings
                        System.out.println("updating");
                        int s=availableSeats(a[7]);
                        Connection conn=getConnection();
                        PreparedStatement stmt=conn.prepareStatement("select No_of_Seats from bookings where  booking_ID = ?");  
                        stmt.setString(1,(String)a[1]);
                        ResultSet rs=stmt.executeQuery();
                        rs.next();
                        int previous=rs.getInt(1);
                        System.out.println("previous"+previous);
                        System.out.println("seats"+s);
                        String message[]=new String[1];
                        if (previous+s>=Integer.parseInt(a[2])){
                            Update(a,"No_of_Seats",2,"C_ID",6);
                            message[0]="Success";
                            out.writeObject(message);
                            System.out.println("success");
                        }
                        else
                        {
                            message[0]="Not Enough";
                            out.writeObject(message);
                            System.out.println("not enough seats");
                        }
                              
                         
                    }
                else if (a[0].equals("updateWaiting"))
                {
                    
                }
                else if (a[0].equals("updateBooking"))
                    {   
                    System.out.print("Updating booking");
                    try {
                    Connection conn=getConnection();
                    PreparedStatement st=conn.prepareStatement("select Customer_ID from customer_info where Email = '"+a[2]+"'"); 
                    ResultSet rs=st.executeQuery();
                    rs.next();
                    String ID=rs.getString(1);
                     String query1="SELECT Excursion_ID FROM excursion WHERE Ex_name = '"+a[4]+"'";
                    ResultSet res=st.executeQuery(query1);
                    res.next();
                    String exID=res.getString(1);
                    System.out.println(exID);
                    int seats=availableSeats(exID);
                    System.out.println("Seats "+seats);
                    System.out.println(a[1]);
                    System.out.println(ID);
                        PreparedStatement stmte=conn.prepareStatement("Select * from bookings WHERE Booking_Status=1 and C_Id = ? and Booking_Id=? ;");  
                        stmte.setString(1,ID);
                        stmte.setString(2,a[5]);
                        ResultSet rms=stmte.executeQuery();
                        String[] b=new String[7];
                        System.out.println(rms.next());
                        System.out.println(a[5]);
                        for (int i = 0;i<7;i++){
                            b[i]=rms.getString(i+1);
                            System.out.println(b[i]);  
                        }
//                    
                    if(Integer.parseInt(b[1])>=Integer.parseInt(a[1])){
                        PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET Waiting_status = 0 WHERE Waiting_status = 1 and C_ID = ? and Ex_ID=? ;");  
                        stt.setString(1,ID);
                        stt.setString(2,exID);
                        stt.executeUpdate();
                        System.out.print("Updating reduced booking");
                        PreparedStatement stmt=conn.prepareStatement("UPDATE bookings SET No_of_seats = ? WHERE Booking_Status=1 and C_Id = ? and Booking_Id=?;");  
                        stmt.setString(1,a[1]);
                        stmt.setString(2,ID);
                         stmt.setString(3,a[5]);
                        stmt.executeUpdate();
                        
                        String []status={"Success"};
                        
                        out.writeObject(status);
                    }
                    else if ((seats+Integer.parseInt(b[1]))>=Integer.parseInt(a[1]))
                    {
                        System.out.print("Updating booking");
                        PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET Waiting_status = 0 WHERE Waiting_status = 1 and C_ID = ? and Ex_Id=? ;");  
                        stt.setString(1,ID);
                        stt.setString(2,exID);
                        stt.executeUpdate();
                        PreparedStatement stmt=conn.prepareStatement("UPDATE bookings SET No_of_seats = ? WHERE Booking_Status=1 and C_Id = ? and Booking_Id=? ;");  
                        stmt.setString(1,a[1]);
                        stmt.setString(2,ID);
                        stmt.setString(3,a[5]);
                        stmt.executeUpdate();
                        

                        String []status={"Success"};

                        out.writeObject(status);
                    }
                    
                    else{
                        PreparedStatement stte=conn.prepareStatement("UPDATE bookings SET Waiting_status = 0 WHERE Waiting_status = 1 and C_ID = ? and Ex_ID=? ;");  
                        stte.setString(1,ID);
                        stte.setString(2,exID);
                        stte.executeUpdate();
                        
                        System.out.println("Previous"+b[1]);
                        PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET No_of_seats = ? WHERE Booking_Status=1 and C_Id = ? and Booking_Id=?;");  
                        stt.setString(1,Integer.toString(seats+Integer.parseInt(b[1])));
                        stt.setString(2,ID);
                        stt.setString(3,a[5]);
                        stt.executeUpdate();
                       

                        String []status={"Waiting",Integer.toString(seats+Integer.parseInt(b[1])),Integer.toString(seats)};
                        out.writeObject(status);

                        ObjectInputStream input=new ObjectInputStream(myClientSocket.getInputStream());
                        String [] n=(String [])input.readObject();
                        System.out.println(n[0]);
           
                        if (n[0].equals("true"))
                        {


                            b[1]=Integer.toString(Integer.parseInt(a[1])-Integer.parseInt(b[1])-seats);
                            b[3]="0";
                            b[4]="1";
                            b[5]=a[2];
                            b[6]=a[4];

                            insert(b);
                        }
                        
                        } 
                           }
                           catch(Exception e){
                               e.printStackTrace();
                           }
                        }

                }
                catch(SocketException e) 
                    { 
                        System.out.print("Client exit:"+myClientSocket.getInetAddress().getHostName());

                    }
                catch(Exception e) 
                    { 
                        e.printStackTrace();                       
                    } 
                
            } 


        } 
    //Adding data into database 
    private String Register(String query) {
        try {
            Connection conn = getConnection();
            Statement st=conn.createStatement();
            st.executeUpdate(query);
            conn.close();
        } 
        catch(SQLException e)
        {   
            
            System.out.println(e.getMessage());
            if (e.getMessage().toLowerCase().contains("'cabin_number'")){
                System.out.println("Cabin exists");
                return "Cabin exists";
                
            }
            else if (e.getMessage().toLowerCase().contains("'email'")){
                System.out.println("Email exists");
                return "Email exists";
               
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
      return "success";
        }
    
    //validating login
    private String Login(String[] a) {
		  try   
                    {
                    Connection conn=getConnection();
                    PreparedStatement stmt=conn.prepareStatement("select Email,Password,Admin_Status from customer_info");
                    ResultSet rs=stmt.executeQuery();  
                    while(rs.next()){  
                        if (a[1].equalsIgnoreCase(rs.getString(1)) && a[2].equals(rs.getString(2))&& rs.getString(3).equals("1")){
                          return "admin";}
                        else if (a[1].equalsIgnoreCase(rs.getString(1)) && a[2].equals(rs.getString(2))){
                          return "true";
                        
                    }

  }  
                    conn.close();
		  } catch (Exception e) 
                  {
                    e.printStackTrace();
                  }
                  return "false";
    }
    //creating a 2 dimensional array to send back to client
    private String[][] View(String a,int column){
                    try {
                    Connection conn=getConnection();
                    PreparedStatement stmt=conn.prepareStatement("select * from "+a);  
                    ResultSet rslt=stmt.executeQuery();
                    PreparedStatement st=conn.prepareStatement("select * from "+a);  
                    ResultSet rs=st.executeQuery(); 
                    int count=0;
                    while (rs.next()) {
                        count++;
                    }
                    
                    int i=0;                   
                    String[][] arr = new String[count][column];
                    while(rslt.next())
                    {
                        for(int j=0;j<column;j++)
                        {
                            arr[i][j] = rslt.getString(j+1);
                        }
                        i++;                    
                    }
                    
                    conn.close();
                    return arr;

		  } catch (Exception e) {
		  e.printStackTrace();
		  }
         return null;         
    }
    private void Update(String a[],String field,int b,String field2, int c){
        
                    try {
                    Connection conn=getConnection();
                    PreparedStatement stmt=conn.prepareStatement("UPDATE bookings SET "+field+" = ? WHERE "+field2+" = ? and Booking_Id = ? ;");  
                    stmt.setString(1,(String)a[b]);
                    stmt.setString(2,(String)a[c]);
                    stmt.setString(3,(String)a[1]);
                    stmt.executeUpdate();
                    }
                    catch(Exception e){
                    e.printStackTrace();}
    }
    //insert bookings
    private void insert(String a[]){
        try {
                    Connection conn=getConnection();
                    Statement st=conn.createStatement();
                    String query="SELECT Customer_ID FROM customer_info WHERE Email = '"+a[5]+"'";
                    ResultSet rs=st.executeQuery(query);
                    rs.next();  
                    String value=rs.getString(1);
                    String query1="SELECT Excursion_ID FROM excursion WHERE Ex_name = '"+a[6]+"'";
                    ResultSet rslt=st.executeQuery(query1);
                    rslt.next();
                    String value2=rslt.getString(1);
                    PreparedStatement stmt=conn.prepareStatement("Insert into bookings(No_of_Seats,Date,Booking_status,Waiting_status,C_ID,Ex_ID) Values(?,?,?,?,?,?)");  
                    stmt.setString(1,(String)a[1]);
                    stmt.setString(2,(String)a[2]);
                    stmt.setString(3,(String)a[3]);
                    stmt.setString(4,(String)a[4]);
                    stmt.setString(5,value);
                    stmt.setString(6,value2);
                    
                    stmt.executeUpdate();
                    }
                    catch(Exception e){
                    e.printStackTrace();}
    }
    //canceling booking
    private void cancel(String email,String excursion){
        try {
           Connection conn=getConnection();
           PreparedStatement st=conn.prepareStatement("Select Customer_ID from customer_info where Email='"+email+"'");  
           ResultSet rs=st.executeQuery();
           PreparedStatement ste=conn.prepareStatement("Select Excursion_ID from excursion where Ex_name=?");
           ste.setString(1,excursion);
           ResultSet res=ste.executeQuery();
           res.next();
           String ExID=res.getString(1);
           
           rs.next();
           String ID=rs.getString(1);
           PreparedStatement stmt=conn.prepareStatement("UPDATE bookings SET Booking_status = 0 WHERE Booking_status = 1 and C_ID = ? and Ex_ID=?;");  
           stmt.setString(1,ID);
           stmt.setString(2,ExID);
           stmt.executeUpdate();
           PreparedStatement stt=conn.prepareStatement("UPDATE bookings SET Waiting_status = 0 WHERE Waiting_status = 1 and C_ID = ? and Ex_ID=?;");  
           stt.setString(1,ID);
           stt.setString(2,ExID);
           stt.executeUpdate();

           }
       catch(Exception e){
           e.printStackTrace();
           }
    }
    //checking seats available
    private int availableSeats(String ExID ){
        try {
            Connection conn=getConnection();
            PreparedStatement stmt=conn.prepareStatement("select No_of_Seats from bookings where Ex_ID =? and Booking_status=1");
            stmt.setString(1,ExID);
            ResultSet rs=stmt.executeQuery();
            int seats=0;
            while(rs.next()){
            seats+=rs.getInt(1);
            }
            return 32-seats;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }
    //generating waiting list
    private String[][] customerWaitingView(String email){
                   
                    try {
                    Connection conn=getConnection();
                    PreparedStatement stmt=conn.prepareStatement("SELECT excursion.Port_ID,excursion.Ex_name, bookings.No_of_Seats\n" +
                    "FROM bookings\n" +
                    "INNER JOIN customer_info ON customer_info.Customer_ID = bookings.C_ID\n" +
                    "INNER JOIN excursion ON bookings.Ex_Id=excursion.Excursion_ID\n" +
                    "where Waiting_status=1 and customer_info.Email=?");
                    stmt.setString(1,email);
                    ResultSet rslt=stmt.executeQuery();

                    int count=0;
                    while (rslt.next()) {
                        count++;
                    }
                    rslt.beforeFirst();
                    int i=0;                   
                    String[][] arr = new String[count][3];
                    while(rslt.next())
                    {
                        for(int j=0;j<3;j++)
                        {
                            arr[i][j] = rslt.getString(j+1);
                            System.out.print(arr[i][j]);   
                        }
                        i++;                    
                    }
                    conn.close();
                    return arr;
                    
		  } catch (Exception e) {
		  e.printStackTrace();
		  }
        return null;
    }
    
    
    private Connection getConnection(){
        
        String url = "jdbc:mysql://localhost:3306/";
        String dbName = "cruise_booking";
        String driver = "com.mysql.jdbc.Driver";
        String userName = "root"; 
        String password = "";
        try {
            Class.forName(driver).newInstance();
            Connection conn;
            conn = DriverManager.getConnection(url+dbName,userName,password);
            return conn;
        } 
        catch(Exception ex) {
           System.out.println("Cannot connect to database");
        }
        return null;
    }
   
} 

