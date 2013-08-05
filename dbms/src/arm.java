import java.io.*;
import java.sql.*;

public class arm {

	public static String sUser;
	public static String sPass;
	
	public static String s1Sup;
	public static String s2Sup;
	public static String s3Sup;
	public static String s3FreqSize;
	public static String s4Sup;
	public static String s4Conf;
	public static String s4FreqSize;
	
	public static void main(String[] args) throws SQLException, IOException {
		try{
			FileInputStream fInput = new FileInputStream("system.in");
			DataInputStream dis = new DataInputStream(fInput);
			BufferedReader br = new BufferedReader( new InputStreamReader(dis) );
			
			String sInput = br.readLine();
			
			String sTemp[] = sInput.split(",");
			sUser = sTemp[0].split("=")[1].trim();
			sPass = sTemp[1].split("=")[1].trim();
			
			sInput = br.readLine();
			s1Sup = sInput.split("=")[1].replace("%", "").trim();
			
			sInput = br.readLine();
			s2Sup = sInput.split("=")[1].replace("%", "").trim();
			
			sInput = br.readLine();
			sTemp = sInput.split(",");
			s3Sup = sTemp[0].split("=")[1].replace("%", "").trim();
			s3FreqSize = sTemp[1].split("=")[1].trim();
			
			sInput = br.readLine();
			sTemp = sInput.split(",");
			s4Sup = sTemp[0].split("=")[1].replace("%", "").trim();
			s4Conf = sTemp[1].split("=")[1].replace("%", "").trim();
			s4FreqSize = sTemp[2].split("=")[1].trim();
			
			dis.close();
		} catch (Exception e){
			System.out.println(e);
		}
		
		
		DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		Connection conn = DriverManager.getConnection ("jdbc:oracle:thin:hr/hr@oracle1.cise.ufl.edu:1521:orcl",sUser,sPass);
		Statement stmt = conn.createStatement ();
		
		//--------------------create tables
		stmt.executeQuery ("create table items ( id varchar(10) primary key, name varchar(50) not null )");
		stmt.executeQuery ("create table trans ( sid varchar(10), id varchar(10) references items(id))");
		
		try{
			FileInputStream fInput = new FileInputStream("items.dat");
			DataInputStream dis = new DataInputStream(fInput);
			BufferedReader br = new BufferedReader( new InputStreamReader(dis) );
			
			String sInput = "";
			while( (sInput = br.readLine() ) != null){
				if(sInput == "")
					break;
				
				String sTemp [] = sInput.split(",");
				stmt.addBatch("insert into items values ("+sTemp[0]+","+sTemp[1]+")");
			}
			
			dis.close();
		} catch (Exception e){
			System.out.println(e);
		}
		
		stmt.executeBatch();
		
		try{
			FileInputStream fInput = new FileInputStream("trans.dat");
			DataInputStream dis = new DataInputStream(fInput);
			BufferedReader br = new BufferedReader( new InputStreamReader(dis) );
			
			String sInput = "";
			while( (sInput = br.readLine() ) != null){
				if(sInput == "")
					break;
				
				String sTemp [] = sInput.split(",");
				stmt.addBatch("insert into trans values ("+sTemp[0]+","+sTemp[1]+")");
				//System.out.println("insert into trans values ("+sTemp[0]+","+sTemp[1]+")");
			}
			
			dis.close();
		} catch (Exception e){
			System.out.println(e);
		}
		
		stmt.executeBatch();
		
		//--------------------------
		
		ResultSet rs = stmt.executeQuery("select count(distinct sid) from trans");
		rs.next();
		int iTotalTrans = rs.getInt(1);
		
		//---------------------------------task 1
		int i1Trs = (int)Math.ceil((double)iTotalTrans*Integer.parseInt(s1Sup)/100);
		//System.out.println("total trans:"+iTotalTrans);
		
		PrintWriter fout = new PrintWriter(new FileWriter("system.out.1"));
		
		rs = stmt.executeQuery("select i.name, count(*) from trans t, items i where t.id = i.id group by i.name having count(*) >= "+i1Trs);
		System.out.println("Task 1: (sup="+i1Trs+")");
				
		while(rs.next()){
			//System.out.println("{"+rs.getString(1)+"}, s="+((double)rs.getInt(2)*100/iTotalTrans)+"%");
			fout.println("{"+rs.getString(1)+"}, s="+((double)rs.getInt(2)*100/iTotalTrans)+"%");
		}
		
		fout.close();
		//-----------------------task 2
		
		int i2Trs = (int)Math.ceil((double)iTotalTrans*Integer.parseInt(s2Sup)/100);
		
		System.out.println("Task 2: (sup="+i2Trs+", size=2)");
		
		rs = stmt.executeQuery("select i.name, count(*) from trans t, items i where t.id = i.id group by i.name having count(*) >= "+i2Trs);
		
		fout = new PrintWriter(new FileWriter("system.out.2")); 
		
		while(rs.next()){
			//System.out.println("{"+rs.getString(1)+"}, s="+((double)rs.getInt(2)*100/iTotalTrans)+"%");
			fout.println("{"+rs.getString(1)+"}, s="+((double)rs.getInt(2)*100/iTotalTrans)+"%");
		}
		stmt.executeQuery("create table f1_trans "+
				"as select t.sid, t.id from trans t where t.id in "+
				"( select id from trans group by id having count(*) >= "+i2Trs+")");
		
		stmt.executeQuery("create table fi_2 as select t1.id as id1, t2.id as id2, count(*) as count from f1_trans t1, f1_trans t2 where t1.sid = t2.sid and t1.id < t2.id group by t1.id, t2.id having count(*) >= "+i2Trs);
		
		rs = stmt.executeQuery("select i.name, i2.name, count from items i, items i2, fi_2 f where i.id=f.id1 and i2.id=f.id2");
		
		while(rs.next()){
			//System.out.println("{"+rs.getString(1)+","+rs.getString(2)+"}, s="+((double)rs.getInt(3)*100/iTotalTrans)+"%");
			fout.println("{"+rs.getString(1)+","+rs.getString(2)+"}, s="+((double)rs.getInt(3)*100/iTotalTrans)+"%");
		}
		
		stmt.executeQuery("drop table f1_trans");
		stmt.executeQuery("drop table fi_2");
		
		fout.close();
		//-------------------------task 3
		
		int i3Trs = (int)Math.ceil((double)iTotalTrans*Integer.parseInt(s3Sup)/100);
		System.out.println("Task 3: (sup="+i3Trs+", size="+s3FreqSize+")");
		
		fout = new PrintWriter(new FileWriter("system.out.3")); 
		
		for(int i = 0;i < Integer.parseInt(s3FreqSize);i++){
			int iCurFI = i+1;
			
			//-------------create fiset cand and check valid
			stmt.executeQuery("create table fiset_"+iCurFI+" (setid int null, id varchar(10), count varchar(10) )");
			
			if(iCurFI == 1){
				stmt.executeQuery("create table trans_1 (sid varchar(10), id varchar(10))");
				stmt.executeQuery("CREATE SEQUENCE seq MINVALUE 1 START WITH 1 INCREMENT BY 1 CACHE 20");
				stmt.executeQuery("CREATE OR REPLACE TRIGGER incre_f1_setid BEFORE INSERT ON fiset_1 FOR EACH ROW BEGIN :new.setid := seq.nextval; END;");
				stmt.executeQuery("insert into fiset_1(id, count) select distinct(id), count(*) from trans group by id having count(*) >= "+i3Trs);
				stmt.executeQuery("drop trigger incre_f1_setid");
				stmt.executeQuery("drop sequence seq");
				
				String sOut = "select i.name, f.count from fiset_1 f, items i where i.id = f.id";
				rs = stmt.executeQuery(sOut);
				while(rs.next()){
					//System.out.println("{"+rs.getString(1)+"}, s="+((double)rs.getInt(2)*100/iTotalTrans)+"%");
					fout.println("{"+rs.getString(1)+"}, s="+((double)rs.getInt(2)*100/iTotalTrans)+"%");
				}
				
				stmt.executeQuery("insert into trans_1 select * from trans where id in (select id from fiset_1)");
			}else{
				stmt.executeQuery("create table fiset_"+iCurFI+"_cand (setid int null, id varchar(10) )");
								
				String sProc = "declare " +
							"cursor c is select distinct is1.setid as id1, is2.setid as id2 from fiset_"+i+" is1, fiset_"+i+" is2 " +
							"where is1.setid > is2.setid and "+iCurFI+" = (select count(distinct id) from fiset_"+i+" is3 where is1.setid = is3.setid or " +
							"is3.setid = is2.setid); " +
							"setp c%rowtype; i number; sc varchar(100); chk integer; " +
							"begin " +
							"	i := 1;" +
							"	sc := '';" +
							"	chk := 0;" +
							"	open c;" +
							"	loop" +
							"		fetch c into setp;" +
							"		exit when c%notfound;" +
							"		select wm_concat(id) into sc from ( select distinct(id) from fiset_"+i+" where setid = setp.id1 or setid = setp.id2 order by id );" +
							"		select count(*) into chk from chk_set where s=sc;" +
							"		if chk != 1 then" +
							"			insert into chk_set values (sc);" +
							"			insert into fiset_"+iCurFI+"_cand(id) select distinct(id) from fiset_"+i+" where setid = setp.id1 or setid = setp.id2;" +
							"			update fiset_"+iCurFI+"_cand set setid = i where setid is null;" +
							"			i := i + 1;" +
							"		end if;" +
							"	end loop;" +
							"	close c;" +
							"end;";
				
				stmt.executeQuery("create table chk_set(s varchar(100))");
				stmt.execute(sProc);
				stmt.executeQuery("drop table chk_set");
				
				rs = stmt.executeQuery("select count(distinct setid) from fiset_"+iCurFI+"_cand");
				rs.next();
				int iSet = rs.getInt(1);
				
				int iNewSet = 1;
				for(int j = 0;j < iSet;j++){
					//System.out.println("prune candidate set");
					stmt.executeQuery("create table temp (id varchar(10))");
					stmt.executeQuery("insert into temp select id from fiset_"+iCurFI+"_cand where setid = "+(j+1));
					String sCheckApri = "select count(*) from ( select f.setid from temp, fiset_"+i+" f where temp.id = f.id group by f.setid having count(*) = " +i+")";
					ResultSet rsApri = stmt.executeQuery(sCheckApri);
					//System.out.println("SQL to prune:"+sCheckApri);
					rsApri.next();
					
					if(rsApri.getInt(1) != iCurFI){
						stmt.executeQuery("drop table temp");
						continue;
					}
					stmt.executeQuery("drop table temp");
					
					ResultSet rsCand = stmt.executeQuery("select id from fiset_"+iCurFI+"_cand where setid = "+(j+1));
					
					String sCheckValid = "select ";
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						sCheckValid += "t"+ik+".id as id"+ik+", ";
					}
					sCheckValid += "count(*) from ";
					
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						sCheckValid += "trans_1 t"+ik+", ";
					}
					sCheckValid = sCheckValid.substring(0,sCheckValid.length()-2);
					sCheckValid += " where ";
					
					for(int k=0;k<(iCurFI-1);k++){
						int ik = k+1;
						sCheckValid += "t"+ik+".sid = t"+(ik+1)+".sid and ";
					}
					
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						rsCand.next();
						sCheckValid += "t"+ik+".id = "+rsCand.getString(1)+" and ";
					}
					sCheckValid = sCheckValid.substring(0,sCheckValid.length()-4);
					sCheckValid += "group by ";
					
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						sCheckValid += "t"+ik+".id, ";
					}
					sCheckValid = sCheckValid.substring(0,sCheckValid.length()-2);
					sCheckValid += " having count(*) >= "+i3Trs;
					
					ResultSet rsValid = stmt.executeQuery(sCheckValid);
					
					if(rsValid.next()){
						for(int k=0;k<iCurFI;k++){
							int ik = k+1;
							String sInsert = "insert into fiset_"+iCurFI+"(id) values ("+rsValid.getString(ik)+")";
							stmt.addBatch(sInsert);
						}
						int iCount = rsValid.getInt(iCurFI+1);
						
						stmt.executeBatch();
						stmt.executeQuery("update fiset_"+iCurFI+" set setid = "+iNewSet+" where setid is null");
						
						ResultSet rsName = stmt.executeQuery("select i.name from items i, fiset_"+iCurFI+" f where f.id=i.id and f.setid = "+iNewSet);
						String sOut = "{";
						while(rsName.next()){
							sOut += rsName.getString(1)+",";
						}
						sOut = sOut.substring(0,sOut.length()-1);
						sOut += "}, s="+((double)iCount*100/iTotalTrans)+"%";
						iNewSet++;
						
						//System.out.println(sOut);
						fout.println(sOut);
					}
				}
				
				stmt.executeQuery("drop table fiset_"+iCurFI+"_cand");
			}
		}
		
		fout.close();

		stmt.executeQuery("DROP TABLE trans_1");
		for(int i = 0;i < Integer.parseInt(s3FreqSize);i++){
			stmt.executeQuery("DROP TABLE fiset_"+(i+1));
		}
		
		//------------------task 4--------------------------------
		int i4Trs = (int)Math.ceil((double)iTotalTrans*Integer.parseInt(s4Sup)/100);
		System.out.println("Task 4: (sup="+i4Trs+", conf="+s4Conf+"%, size="+s4FreqSize+")");
		
		fout = new PrintWriter(new FileWriter("system.out.4"));
		
		for(int i = 0;i < Integer.parseInt(s4FreqSize);i++){
			int iCurFI = i+1;
			
			//-------------create fiset cand and check valid
			stmt.executeQuery("create table fiset_"+iCurFI+" (setid int null, id varchar(10), count varchar(10) )");
			
			if(iCurFI == 1){
				stmt.executeQuery("create table trans_1 (sid varchar(10), id varchar(10))");
				stmt.executeQuery("CREATE SEQUENCE seq MINVALUE 1 START WITH 1 INCREMENT BY 1 CACHE 20");
				stmt.executeQuery("CREATE OR REPLACE TRIGGER incre_f1_setid BEFORE INSERT ON fiset_1 FOR EACH ROW BEGIN :new.setid := seq.nextval; END;");
				stmt.executeQuery("insert into fiset_1(id, count) select distinct(id), count(*) from trans group by id having count(*) >= "+i4Trs);
				stmt.executeQuery("drop trigger incre_f1_setid");
				stmt.executeQuery("drop sequence seq");
				
				stmt.executeQuery("insert into trans_1 select * from trans where id in (select id from fiset_1)");
			}else{
				stmt.executeQuery("create table fiset_"+iCurFI+"_cand (setid int null, id varchar(10) )");
								
				String sProc = "declare " +
							"cursor c is select distinct is1.setid as id1, is2.setid as id2 from fiset_"+i+" is1, fiset_"+i+" is2 " +
							"where is1.setid > is2.setid and "+iCurFI+" = (select count(distinct id) from fiset_"+i+" is3 where is1.setid = is3.setid or " +
							"is3.setid = is2.setid); " +
							"setp c%rowtype; i number; sc varchar(100); chk integer; " +
							"begin " +
							"	i := 1;" +
							"	sc := '';" +
							"	chk := 0;" +
							"	open c;" +
							"	loop" +
							"		fetch c into setp;" +
							"		exit when c%notfound;" +
							"		select wm_concat(id) into sc from ( select distinct(id) from fiset_"+i+" where setid = setp.id1 or setid = setp.id2 order by id );" +
							"		select count(*) into chk from chk_set where s=sc;" +
							"		if chk != 1 then" +
							"			insert into chk_set values (sc);" +
							"			insert into fiset_"+iCurFI+"_cand(id) select distinct(id) from fiset_"+i+" where setid = setp.id1 or setid = setp.id2;" +
							"			update fiset_"+iCurFI+"_cand set setid = i where setid is null;" +
							"			i := i + 1;" +
							"		end if;" +
							"	end loop;" +
							"	close c;" +
							"end;";
				
				stmt.executeQuery("create table chk_set(s varchar(100))");
				stmt.execute(sProc);
				stmt.executeQuery("drop table chk_set");
				
				rs = stmt.executeQuery("select count(distinct setid) from fiset_"+iCurFI+"_cand");
				rs.next();
				int iSet = rs.getInt(1);
				
				int iNewSet = 1;
				for(int j = 0;j < iSet;j++){
					stmt.executeQuery("create table temp (id varchar(10))");
					stmt.executeQuery("insert into temp select id from fiset_"+iCurFI+"_cand where setid = "+(j+1));
					String sCheckApri = "select count(*) from ( select f.setid from temp, fiset_"+i+" f where temp.id = f.id group by f.setid having count(*) = " +i+")";
					ResultSet rsApri = stmt.executeQuery(sCheckApri);
					rsApri.next();
					
					if(rsApri.getInt(1) != iCurFI){
						stmt.executeQuery("drop table temp");
						continue;
					}
					stmt.executeQuery("drop table temp");
					
					ResultSet rsCand = stmt.executeQuery("select id from fiset_"+iCurFI+"_cand where setid = "+(j+1));
					
					String sCheckValid = "select ";
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						sCheckValid += "t"+ik+".id as id"+ik+", ";
					}
					sCheckValid += "count(*) from ";
					
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						sCheckValid += "trans_1 t"+ik+", ";
					}
					sCheckValid = sCheckValid.substring(0,sCheckValid.length()-2);
					sCheckValid += " where ";
					
					for(int k=0;k<(iCurFI-1);k++){
						int ik = k+1;
						sCheckValid += "t"+ik+".sid = t"+(ik+1)+".sid and ";
					}
					
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						rsCand.next();
						sCheckValid += "t"+ik+".id = "+rsCand.getString(1)+" and ";
					}
					sCheckValid = sCheckValid.substring(0,sCheckValid.length()-4);
					sCheckValid += "group by ";
					
					for(int k=0;k<iCurFI;k++){
						int ik = k+1;
						sCheckValid += "t"+ik+".id, ";
					}
					sCheckValid = sCheckValid.substring(0,sCheckValid.length()-2);
					sCheckValid += " having count(*) >= "+i4Trs;
					
					ResultSet rsValid = stmt.executeQuery(sCheckValid);
					
					if(rsValid.next()){
						for(int k=0;k<iCurFI;k++){
							int ik = k+1;
							String sInsert = "insert into fiset_"+iCurFI+"(id) values ("+rsValid.getString(ik)+")";
							stmt.addBatch(sInsert);
						}
						int iCount = rsValid.getInt(iCurFI+1);
						
						stmt.executeBatch();
						stmt.executeQuery("update fiset_"+iCurFI+" set setid = "+iNewSet+" where setid is null");
						stmt.executeQuery("update fiset_"+iCurFI+" set count = "+iCount+" where count is null");
						
						iNewSet++;
					}
				}
				
				stmt.executeQuery("drop table fiset_"+iCurFI+"_cand");
			}
		}
		
		//-------------finding ar
		for(int i = 1;i < Integer.parseInt(s4FreqSize);i++){
			int iCurFI = i+1;
			
			rs = stmt.executeQuery("select count(distinct setid) from fiset_"+iCurFI);
			rs.next();
			int iSet = rs.getInt(1);
			
			for( int j = 0;j<iSet;j++){
				int iCurSet = j+1;
				
				ResultSet rsSup = stmt.executeQuery("select distinct count from fiset_"+iCurFI+" where setid = "+iCurSet);
				rsSup.next();
				double dSup = ((double)rsSup.getInt(1)*100/iTotalTrans);
				
				stmt.executeQuery("create table temp_1(setid varchar(10), id varchar(10))");
				stmt.executeQuery("CREATE SEQUENCE seq MINVALUE 1 START WITH 1 INCREMENT BY 1 CACHE 20");
				stmt.executeQuery("CREATE OR REPLACE TRIGGER incre_temp_1_setid BEFORE INSERT ON temp_1 FOR EACH ROW BEGIN :new.setid := seq.nextval; END;");
				stmt.executeQuery("insert into temp_1(id) select id from fiset_"+iCurFI+" where setid = "+iCurSet);
				stmt.executeQuery("drop trigger incre_temp_1_setid");
				stmt.executeQuery("drop sequence seq");
								
				for( int k = 1;k<iCurFI-1;k++){
					int iCurSub = k+1;
					
					stmt.executeQuery("create table temp_"+iCurSub+"(setid varchar(10), id varchar(10))");
					
					String sProc = "declare " +
							"cursor c is select distinct is1.setid as id1, is2.setid as id2 from temp_"+k+" is1, temp_"+k+" is2 " +
							"where is1.setid > is2.setid and "+iCurSub+" = (select count(distinct id) from temp_"+k+" is3 where is1.setid = is3.setid or " +
							"is3.setid = is2.setid); " +
							"setp c%rowtype; i number; sc varchar(100); chk integer; " +
							"begin " +
							"	i := 1;" +
							"	sc := '';" +
							"	chk := 0;" +
							"	open c;" +
							"	loop" +
							"		fetch c into setp;" +
							"		exit when c%notfound;" +
							"		select wm_concat(id) into sc from ( select distinct(id) from temp_"+k+" where setid = setp.id1 or setid = setp.id2 order by id );" +
							"		select count(*) into chk from chk_set where s=sc;" +
							"		if chk != 1 then" +
							"			insert into chk_set values (sc);" +
							"			insert into temp_"+iCurSub+"(id) select distinct(id) from temp_"+k+" where setid = setp.id1 or setid = setp.id2;" +
							"			update temp_"+iCurSub+" set setid = i where setid is null;" +
							"			i := i + 1;" +
							"		end if;" +
							"	end loop;" +
							"	close c;" +
							"end;";
				
					stmt.executeQuery("create table chk_set(s varchar(100))");
					stmt.execute(sProc);
					stmt.executeQuery("drop table chk_set");
				}
				
				for( int k = 0;k<iCurFI-1;k++){
					int iCurSub = k+1;
					
					ResultSet rsSet = stmt.executeQuery("select count(distinct setid) from temp_"+iCurSub);
					rsSet.next();
					int iTotalSet = rsSet.getInt(1);
					
					for( int l = 0;l<iTotalSet;l++){
						int iCurSubSet = l+1;
						
						stmt.executeQuery("create table temp_trans(sid varchar(10), id varchar(10))");
						
						String sTransofSub = "insert into temp_trans select sid, id from trans where sid in (select t0.sid from ";
						
						for(int m = 0 ; m < iCurSub ; m++){
							sTransofSub += "trans t"+m+", ";
						}
						sTransofSub = sTransofSub.substring(0, sTransofSub.length()-2);
						sTransofSub += " where ";
						
						for(int m = 0 ; m < iCurSub-1 ; m++){
							sTransofSub += "t"+m+".sid = t"+(m+1)+".sid and ";
						}
						
						ResultSet rsID = stmt.executeQuery("select id from temp_"+iCurSub+" where setid = "+iCurSubSet);
						
						for(int m = 0 ; m < iCurSub ; m++){
							rsID.next();
							sTransofSub += "t"+m+".id = "+rsID.getInt(1)+" and ";
						}
						sTransofSub = sTransofSub.substring(0, sTransofSub.length()-4);
						sTransofSub += ")";
						
						stmt.executeQuery(sTransofSub);
						
						ResultSet rsTotalCount = stmt.executeQuery("select count(distinct sid) from temp_trans");
						rsTotalCount.next();
						int iCurSubSetCount = rsTotalCount.getInt(1);
						
						String sCountConf = "select count(distinct t0.sid) from ";
						
						for(int m = 0 ; m < iCurFI-iCurSub ; m++){
							sCountConf += "temp_trans t"+m+", ";
						}
						sCountConf = sCountConf.substring(0, sCountConf.length()-2);
						sCountConf += " where ";
						
						ResultSet rsOutID = stmt.executeQuery("(select id from temp_1) minus (select id from temp_"+iCurSub+" where setid = "+iCurSubSet+")");
						for(int m = 0 ; m < iCurFI-iCurSub ; m++){
							rsOutID.next();
							sCountConf += "t"+m+".id = "+rsOutID.getString(1)+" and ";
						}
						
						for(int m = 0 ; m < iCurFI-iCurSub-1 ; m++){
							sCountConf += "t"+m+".sid = t"+(m+1)+".sid and ";
						}
						
						sCountConf = sCountConf.substring(0, sCountConf.length()-4);
						ResultSet rsCount = stmt.executeQuery(sCountConf);
						rsCount.next();
						
						int iSubConf = rsCount.getInt(1);
						if( ((double)iSubConf/iCurSubSetCount) >= ((double)Integer.parseInt(s4Conf)/100) ){
							
							ResultSet rsName = stmt.executeQuery("select i.name from items i, temp_"+iCurSub+" t where t.setid = "+iCurSubSet+" and i.id = t.id");
							String sOut = "{{";
							
							while(rsName.next()){
								sOut += rsName.getString(1)+", ";
							}
							sOut = sOut.substring(0, sOut.length()-2);
							sOut += "}->{";
							
							rsName = stmt.executeQuery("select i.name from items i where i.id in ((select id from temp_1) minus (select id from temp_"+iCurSub+" where setid = "+iCurSubSet+"))");
							while(rsName.next()){
								sOut += rsName.getString(1)+", ";
							}
							sOut = sOut.substring(0, sOut.length()-2);
							sOut += "}}, s="+dSup+"%, c="+((double)iSubConf*100/iCurSubSetCount)+"%";
							
							fout.println(sOut);
						}
						stmt.executeQuery("drop table temp_trans");
					}
				}
				
				for( int k = 0;k<iCurFI-1;k++){
					int lk = k+1;
					stmt.executeQuery("drop table temp_"+lk);
				}
			}
			
		}
		//-----------------------
		
		fout.close();
		stmt.executeQuery("DROP TABLE trans_1");
		for(int i = 0;i < Integer.parseInt(s4FreqSize);i++){
			stmt.executeQuery("DROP TABLE fiset_"+(i+1));
		}
		//------------------end task
		stmt.executeQuery("DROP TABLE trans");
		stmt.executeQuery("DROP TABLE items");
		stmt.executeQuery("PURGE USER_RECYCLEBIN");
		
		conn.close();
		
		System.out.println("All done!!!");
	}

}
