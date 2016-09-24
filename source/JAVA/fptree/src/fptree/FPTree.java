package fptree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FPTree {

	private int minSuport;

	public int getMinSuport() {
		return minSuport;
	}

	public void print(String S) {
		System.out.print(S);
	}

	public void setMinSuport(int minSuport) {
		this.minSuport = minSuport;
	}

	// �����ɸ��ļ��ж���Transaction Record
	public List<List<String>> readTransRocords(String... filenames) {
		List<List<String>> transaction = null;
		if (filenames.length > 0) {
			transaction = new LinkedList<List<String>>();
			for (String filename : filenames) {
				try {
					FileReader fr = new FileReader(filename);
					BufferedReader br = new BufferedReader(fr);
					try {
						String line;
						List<String> record;
						while ((line = br.readLine()) != null) {
							if (line.trim().length() > 0) {
								String str[] = line.split(",");
								record = new LinkedList<String>();
								for (String w : str)
									record.add(w);
								transaction.add(record);
							}
						}
					} finally {
						br.close();
					}
				} catch (IOException ex) {
					System.out.println("Read transaction records failed."
							+ ex.getMessage());
					System.exit(1);
				}
			}
		}
		return transaction;
	}

	// FP-Growth�㷨
	
	/* transRecordsΪ����Դ
	 * items�ֵ���ÿ����Ʒ�ۼƼ���
	 * lenΪ��Ч���������������֧�ֶȵ�
	 * dΪ֧�ֶ�Լ��
	 * dateΪ���ݴ�������
	 * isdatabase�ж��Ƿ��Ǵ�Զ�����ݿ������Ʒ����
	 * 
	 * */
	public void FPGrowth(List<List<String>> transRecords, int len,
			Map<String, Integer> items, List<String> postPattern, double d,
			String storeName, Date date,int isdatabase) throws SQLException {
		// ������ͷ����ͬʱҲ��Ƶ��1�
		ArrayList<TreeNode> HeaderTable = buildHeaderTable(transRecords);
		// ����FP-Tree
		TreeNode treeRoot = buildFPTree(transRecords, HeaderTable);
		// ���FP-TreeΪ���򷵻�
		if (treeRoot.getChildren() == null
				|| treeRoot.getChildren().size() == 0)
			return;
		// �����ͷ����ÿһ��+postPattern
		if (postPattern != null) {
			if (postPattern.size() == 1) {
				for (TreeNode header : HeaderTable) {
					String left = header.getName();
					int all = header.getCount();
					String right = postPattern.get(0);
					int leftno = items.get(left);
					int rightno = items.get(right);
					double support = (double) all / len;
					if (support < d) {
						continue;
					}
					double confidentlr = (double) all / leftno;
					double confidentrl = (double) all / rightno;
					double rise = support
							/ (((double) leftno / len) * ((double) rightno / len));
					String l="",r="";
					try{
					if(isdatabase==0){
						l = finditem(left);
						r = finditem(right);}
					else{
					//	l = finditemfromdatabase(left);
					//	r = finditemfromdatabase(right);
					// ��Ϊ��������ֻҪ���Ⱥ���Ŀ�����Բ��ò������ݿ⣬�Լ�����
						l=finddfn(left);
						r=finddfn(right);
					}}
					catch(Exception e){
						continue;
					}
					insertresult(l, r, support, confidentlr, rise, storeName,
							date, leftno, rightno, all, left, right);
					insertresult(r, l, support, confidentrl, rise, storeName,
							date, rightno, leftno, all, right, left);
					print(left + "==>" + right + "\t" + support + "\t"
							+ confidentlr + "\t" + rise + "\n");
					System.out.println();
					print(right + "==>" + left + "\t" + support + "\t"
							+ confidentrl + "\t" + rise + "\n");
					System.out.println();
				}
			} else {
				return;
			}
		} else {
			for (TreeNode header : HeaderTable) {
				items.put(header.getName(), header.getCount());
				// System.out.print(header.getCount() + "\t" + header.getName()
				// + "\n");
			}
			// System.out.println("---------------");
		}
		// �ҵ���ͷ����ÿһ�������ģʽ��������ݹ����
		for (TreeNode header : HeaderTable) {
			// ��׺ģʽ����һ��
			List<String> newPostPattern = new LinkedList<String>();
			newPostPattern.add(header.getName());
			if (postPattern != null)
				newPostPattern.addAll(postPattern);
			// Ѱ��header������ģʽ��CPB������newTransRecords��
			List<List<String>> newTransRecords = new LinkedList<List<String>>();
			TreeNode backnode = header.getNextHomonym();
			while (backnode != null) {
				int counter = backnode.getCount();
				List<String> prenodes = new ArrayList<String>();
				TreeNode parent = backnode;
				// ����backnode�����Ƚڵ㣬�ŵ�prenodes��
				while ((parent = parent.getParent()).getName() != null) {
					prenodes.add(parent.getName());
				}
				while (counter-- > 0) {
					newTransRecords.add(prenodes);
				}
				backnode = backnode.getNextHomonym();
			}
			// �ݹ����
			FPGrowth(newTransRecords, len, items, newPostPattern, d, storeName,
					date,isdatabase);
		}
	}

	// ������ͷ����ͬʱҲ��Ƶ��1�
	public ArrayList<TreeNode> buildHeaderTable(List<List<String>> transRecords) {
		ArrayList<TreeNode> F1 = null;
		if (transRecords.size() > 0) {
			F1 = new ArrayList<TreeNode>();
			Map<String, TreeNode> map = new HashMap<String, TreeNode>();
			// �����������ݿ��и����֧�ֶ�
			for (List<String> record : transRecords) {
				for (String item : record) {
					if (!map.keySet().contains(item)) {
						TreeNode node = new TreeNode(item);
						node.setCount(1);
						map.put(item, node);
					} else {
						map.get(item).countIncrement(1);
					}
				}
			}
			// ��֧�ֶȴ��ڣ�����ڣ�minSup������뵽F1��
			Set<String> names = map.keySet();
			for (String name : names) {
				TreeNode tnode = map.get(name);
				if (tnode.getCount() >= minSuport) {
					F1.add(tnode);
				}
			}
			Collections.sort(F1);
			return F1;
		} else {
			return null;
		}
	}

	// ����FP-Tree
	public TreeNode buildFPTree(List<List<String>> transRecords,
			ArrayList<TreeNode> F1) {
		TreeNode root = new TreeNode(); // �������ĸ��ڵ�
		for (List<String> transRecord : transRecords) {
			LinkedList<String> record = sortByF1(transRecord, F1);
			TreeNode subTreeRoot = root;
			TreeNode tmpRoot = null;
			if (root.getChildren() != null) {
				while (!record.isEmpty()
						&& (tmpRoot = subTreeRoot.findChild(record.peek())) != null) {
					tmpRoot.countIncrement(1);
					subTreeRoot = tmpRoot;
					record.poll();
				}
			}
			addNodes(subTreeRoot, record, F1);
		}
		return root;
	}

	// �ѽ��׼�¼�����Ƶ������������
	public LinkedList<String> sortByF1(List<String> transRecord,
			ArrayList<TreeNode> F1) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (String item : transRecord) {
			// ����F1�Ѿ��ǰ��������еģ����������ǲ�����Ҫ
			for (int i = 0; i < F1.size(); i++) {
				TreeNode tnode = F1.get(i);
				if (tnode.getName().equals(item)) {
					map.put(item, i);
				}
			}
		}
		ArrayList<Entry<String, Integer>> al = new ArrayList<Entry<String, Integer>>(
				map.entrySet());
		Collections.sort(al, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> arg0,
					Entry<String, Integer> arg1) {
				// ��������
				return arg0.getValue() - arg1.getValue();
			}
		});
		LinkedList<String> rest = new LinkedList<String>();
		for (Entry<String, Integer> entry : al) {
			rest.add(entry.getKey());
		}
		return rest;
	}

	// ��record��Ϊancestor�ĺ����������
	public void addNodes(TreeNode ancestor, LinkedList<String> record,
			ArrayList<TreeNode> F1) {
		if (record.size() > 0) {
			while (record.size() > 0) {
				String item = record.poll();
				TreeNode leafnode = new TreeNode(item);
				leafnode.setCount(1);
				leafnode.setParent(ancestor);
				ancestor.addChild(leafnode);

				for (TreeNode f1 : F1) {
					if (f1.getName().equals(item)) {
						while (f1.getNextHomonym() != null) {
							f1 = f1.getNextHomonym();
						}
						f1.setNextHomonym(leafnode);
						break;
					}
				}

				addNodes(leafnode, record, F1);
			}
		}
	}

	/*
	 * ͨ����Ʒ����ҵ����µı���
	 */
	public String finditem(String itemno) throws SQLException {
		ResultSet s = null;
		Mysql ss = null;
		try {
			ss = new Mysql();
			String sql = "SELECT distinct title FROM qingmu.tmp_order_detail_for_skx where merchant_sn like '"
					+ itemno + "%' order by import_date desc limit 1;";
			s = ss.exectequery(sql);
			while (s.next()) {
				return s.getString(1);
			}

		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			s.close();
			ss.closeall();
		}
		return null;

	}
	
	/*
	 * �����ݿ���ͨ����Ʒ����ҵ����µı���
	 */
	public String finditemfromdatabase(String itemno) throws SQLException {
		ResultSet s = null;
		Mysql ss = null;
		try {
			String url = "jdbc:mysql://192.168.0.26:3306/orders_db?"
					+ "user=fenxi&password=passowrd_for_orders_db&useUnicode=true&characterEncoding=UTF8";
			ss = new Mysql(url);
			String sql = "SELECT distinct title FROM orders_db.t_order_detail_tm where merchant_sn like '"
					+ itemno + "%' order by import_date desc limit 1;";
			s = ss.exectequery(sql);
			while (s.next()) {
				return s.getString(1);
			}

		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			s.close();
			ss.closeall();
		}
		return null;

	}
	
	// ͨ����λ��Ų�����Ʒ���
	public String finddfn(String code){
		String name="";
		//������
		String jijie=code.substring(0, 1);
		//�����
		String leibie=code.substring(1);
		switch(Integer.parseInt(jijie)){
		case 1:name+="����-";break;
		case 2:name+="����-";break;
		case 3:name+="����-";break;
		case 4:name+="�＾-";break;
		case 5:name+="����-";break;
		case 6:name+="����-";break;	
		}
		switch(Integer.parseInt(leibie)){
		case 1:name+="ŮЬ+����";break;
		case 2:name+="ŮЬ+�п�";break;
		case 3:name+="ŮЬ+��Ь";break;
		case 4:name+="ŮЬ+�ﵥ";break;
		case 5:name+="ŮЬ+��Ƥѥ";break;
		case 7:name+="ŮЬ+������ѥ";break;
		case 8:name+="ŮЬ+��ë��ѥ";break;
		case 11:name+="��Ь+����";break;
		case 13:name+="��Ь+��Ь";break;
		case 14:name+="��Ь+�ﵥ";break;
		case 15:name+="��Ь+��Ƥѥ";break;
		case 17:name+="��Ь+������ѥ";break;
		case 18:name+="��Ь+��ë��ѥ";break;
		case 21:name+="��ͯЬ+����";break;
		case 23:name+="��ͯЬ+��Ь";break;
		case 24:name+="��ͯЬ+�ﵥ";break;
		case 25:name+="��ͯЬ+��Ƥѥ";break;
		case 27:name+="��ͯЬ+������ѥ";break;
		case 28:name+="��ͯЬ+��ë��ѥ";break;
		case 31:name+="ŮͯЬ+����";break;
		case 33:name+="ŮͯЬ+��Ь";break;
		case 34:name+="ŮͯЬ+�ﵥ";break;
		case 35:name+="ŮͯЬ+��Ƥѥ";break;
		case 37:name+="ŮͯЬ+������ѥ";break;
		case 38:name+="ŮͯЬ+��ë��ѥ";break;
		case 10:name+="������Ь+������";break;
		case 20:name+="������Ь+Ů����";break;
		case 30:name+="������Ь+ͯ����";break;
		case 40:name+="������Ь+������";break;
		case 50:name+="������Ь+Ů����";break;
		case 60:name+="������Ь+ͯ����";break;
		case 61:name+="��װ+����";break;
		case 62:name+="��װ+ë��";break;
		case 63:name+="��װ+����֯����";break;
		case 64:name+="��װ+����/PU�п�";break;
		case 65:name+="��װ+T��";break;
		case 66:name+="��װ+����/����";break;
		case 67:name+="��װ+���޷�";break;
		case 68:name+="��װ+Ƥë��";break;
		case 69:name+="��װ+����";break;
		case 70:name+="��װ+����ȹ";break;
		case 71:name+="��װ+ȹ��";break;
		case 72:name+="��װ+ţ�п�";break;
		case 73:name+="��װ+����/��׿�";break;
		case 74:name+="��װ+�ڿ�";break;
		case 81:name+="����+�۾�";break;
		case 82:name+="����+Ǯ��";break;
		case 83:name+="����+�ִ�/��";break;
		case 84:name+="����+�ֱ�";break;
		case 85:name+="����+����";break;
		case 86:name+="����+ñ��";break;
		case 87:name+="����+����";break;
		case 88:name+="����+Χ��/˿��";break;
		case 89:name+="����+Ƥ��/����";break;
		case 90:name+="����+����";break;
		case 91:name+="����+����";break;
		case 92:name+="����+����/����";break;
		case 93:name+="����+��ָ/ָ��";break;
		case 94:name+="����+����";break;
		}
		return name;
	}

	public void insertresult(String left, String right, double s, double c,
			double l, String storeName, Date date, int leftno, int rightno,
			int totalno, String leftcode, String rightcode) throws SQLException {
		PreparedStatement p = null;
		Mysql ss = null;
		try {
			ss = new Mysql();
			String sql = "INSERT INTO `linkresult`(`left`,`right`,`support`,`confident`,`lift`,`dates`,`storesname`,`leftno`,`rightno`,`totalno`,`leftcode`,`rightcode`) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
			p = ss.conn.prepareStatement(sql);
			p.setString(1, left);
			p.setString(2, right);
			p.setDouble(3, s);
			p.setDouble(4, c);
			p.setDouble(5, l);
			p.setString(7, storeName);
			p.setDate(6, new java.sql.Date(date.getTime()));
			p.setInt(8, leftno);
			p.setInt(9, rightno);
			p.setInt(10, totalno);
			p.setString(11, leftcode);
			p.setString(12, rightcode);
			p.execute();

		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			p.close();
			ss.closeall();
		}
	}

	/*����ֻ�ܽ���һ�����ݴ�������ɾ����������*/
	public static void deleteresult(String storeName, Date date)
			throws SQLException {
		PreparedStatement p = null;
		Mysql ss = null;
		try {
			ss = new Mysql();
			String sql = "delete from `linkresult` where storesname=? and dates=?";
			p = ss.conn.prepareStatement(sql);
			p.setString(1, storeName);
			p.setDate(2, new java.sql.Date(date.getTime()));
			p.execute();
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			p.close();
			ss.closeall();
		}
	}
	
	// �����ݿ����ȡ��ܽ�����ݣ�shop_idΪ����id
	public  static List<List<String>> getDataFromMysql(String url,String shop_id) throws SQLException, ClassNotFoundException{
		List<List<String>> transaction;
		transaction = new LinkedList<List<String>>();
		Mysql db=new Mysql(url);
		String selectsql="SELECT group_concat(merchant_sn) as products FROM t_order_detail_tm "
				+ "where order_status=1 and shop_id="+shop_id+" group by order_sn having count(*)>=2";
		ResultSet result = db.exectequery(selectsql);
		while(result.next()){
			List<String> record;
			String line=result.getString(1).trim();
			String str[] = line.split(",");
			record = new LinkedList<String>();
			for (String w : str){
				//�����Ų���16λ�����������쳣����
				if(w.length()!=16){
					continue;
				}
				//��ȡǰ����λ��ż��ɣ������֮ǰ
				String deal=w.substring(4, 7);
				if(record.contains(deal)==false){
					record.add(deal);
				}
			}
			//����ͬһ��Ļ����޳�������
			if(record.size()==1){
				continue;
			}
//			for(String i:record){
//				System.out.print(i+",");
//			}
//			System.out.println();
			transaction.add(record);
		}
		db.closeall();
		return transaction;
	}
	
	// ����ԴΪ�ļ��Ĳ��Է���
	public static void filetest() {
		long startTime = System.currentTimeMillis(); // ��ȡ��ʼʱ��

		Date date = new Date();
		// ������ ��ܽ�ݹٷ��콢��id1,Ь��ٷ��콢��id31
		//����ID
		String shop_id = "1";
		// ���̶�������
		String datass = "src/fptree/dafuni.csv";

		/* ���²��ø� */
		try {
			deleteresult(shop_id, date);
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("ɾ������");
		}
		
		//��Ҫ����
		FPTree fptree = new FPTree();
		fptree.setMinSuport(1);
		List<List<String>> transRecords = fptree.readTransRocords(datass);
		//��Ҫ�������
		
		int len = transRecords.size();
		System.out.println("�ܶ�����:"+len + "���\t֧�ֶ�\t���Ŷ�\t������\n");
		Map<String, Integer> items = new HashMap<String, Integer>();
		
		try {
			fptree.FPGrowth(transRecords, len, items, null, 0.001, shop_id,
					date,0);
		} catch (SQLException e) {
			System.out.println("����");
			e.printStackTrace();
		}
		
		
		long endTime = System.currentTimeMillis(); // ��ȡ����ʱ��
		System.out.println("��������ʱ�䣺 " + (endTime - startTime) / 1000 + "s");
	}
	
	// ����ԴΪ���ݿ�Ĳ��Է���
		public static void databasetest(double support) {
			long startTime = System.currentTimeMillis(); // ��ȡ��ʼʱ��

			Date date = new Date();
			// ������ ��ܽ�ݹٷ��콢��id1,Ь��ٷ��콢��id31
			String url = "jdbc:mysql://192.168.0.26:3306/orders_db?"
					+ "user=fenxi&password=passowrd_for_orders_db&useUnicode=true&characterEncoding=UTF8";
			//String url = "jdbc:mysql://localhost:3306/qingmu?"
			//		+ "user=root&password=6833066&useUnicode=true&characterEncoding=UTF8";
			//����ID
			String shop_id = "1";
			/* ���²��ø� */
			try {
				deleteresult(shop_id, date);
			} catch (SQLException e) {
				e.printStackTrace();
				System.out.println("ɾ������");
			}
			
			//��Ҫ����
			FPTree fptree = new FPTree();
			fptree.setMinSuport(1);
			List<List<String>> transRecords = null;
			try {
				transRecords = getDataFromMysql(url, shop_id);
			} catch (ClassNotFoundException | SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//��Ҫ�������
			
			int len = transRecords.size();
			System.out.println("�ܶ�����:"+len + "���\t֧�ֶ�\t���Ŷ�\t������\n");
			Map<String, Integer> items = new HashMap<String, Integer>();
			
			try {
				fptree.FPGrowth(transRecords, len, items, null, support, shop_id,
						date,1);
			} catch (SQLException e) {
				System.out.println("����");
				e.printStackTrace();
			}
			
			
			long endTime = System.currentTimeMillis(); // ��ȡ����ʱ��
			System.out.println("��������ʱ�䣺 " + (endTime - startTime) / 1000 + "s");
		}
		
	public static void main(String a[]){
		//filetest();
		double support=0.0001;
		databasetest(support);
	}
}