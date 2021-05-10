package ntust.huiting.edgecomputing.v2;

import ntust.huiting.random.Generator;

/**
 * 邊緣運算模擬器
 * @author hui
 *
 */
public class Emulator {
	final static int queue_number = EmulatorParameters.queue_number;
	final static int constant_deadline_time = EmulatorParameters.constant_deadline_time;
	final static int service_number = EmulatorParameters.service_number;
	
	//final static double lambda = 2; // UE傳送到Edge的封包到達率
//	final static int service_number = 3; // 應用類型數量	
	final static double lambda[] = {105, 30, 15}; // 每個應用的平均封包到達率	
	final static double length_packet_UE[] = {0.2, 500, 75}; // 每個應用的平均封包長度(單位: bytes)
	final static double length_packet_E[] = {11, 1, 0.5}; // 每個應用的平均封包長度(單位: bytes)
	final static double length_packet_EU[] = {0.2, 500, 75}; // 每個應用的平均封包長度(單位: bytes)
	final static double bandwidth_UE = 40000; // UE傳送到Edge的頻寬(單位: bytes/seconds)
	final static double bandwidth_E = 4000; // 在Edge的頻寬(單位: bytes/seconds)
	final static double bandwidth_EU = 63000; // Edge傳送到UE的頻寬(單位: bytes/seconds)	
	final static double mu_UE[] = {bandwidth_UE/length_packet_UE[0], bandwidth_UE/length_packet_UE[1], bandwidth_UE/length_packet_UE[2]}; // UE傳送到Edge的每個應用平均服務率(傳輸率)
	final static double mu_E[] = {bandwidth_E/length_packet_E[0], bandwidth_E/length_packet_E[1], bandwidth_E/length_packet_E[2]}; // 在Edge的每個應用平均服務率(傳輸率)
	final static double mu_EU[] = {bandwidth_EU/length_packet_EU[0], bandwidth_EU/length_packet_EU[1], bandwidth_EU/length_packet_EU[2]}; // Edge傳送到UE的每個應用平均服務率(傳輸率)	
	final static int number_packet = 10000; // 模擬封包數	
	static Packet packet[] = new Packet[number_packet]; // 封包
	
	/**
	 * 主程式
	 * @param args
	 */
	public static void main(String args[]) {
		// 初始化封包
		initial_packet();
		
		for(int k = 0; k < queue_number; k++) {
			// M/M/1 queue
			  m_m_1(k);
//			m_m_1_sjf(k);
//			m_m_1_edf(k);
		}
		
		// 顯示統計結果
		summary();
	}
	
	/**
	 * 初始化封包
	 * 設定每個封包的到達時間和服務時間
	 */
	private static void initial_packet() {
		// 亂數產生器
		Generator g = new Generator();
		
		// 當下時間點
		double current_time[] = {0, 0, 0};
		
		//產生number_packet個封包
		for(int i = 0; i < number_packet; i++) {
			int service_type_gen = 0;
			double current_time_gen = current_time[service_type_gen];
			for(int j = 0; j < service_number; j++) {
				if(current_time_gen > current_time[j]) {
					service_type_gen = j;
					current_time_gen = current_time[service_type_gen];
				}
			}
			double inter_arrival_time = g.getRandomNumber_Exponential(lambda[service_type_gen]);
			double arrival_time = current_time[service_type_gen] += inter_arrival_time;
			double service_time[] = new double[queue_number];
			service_time[0] = g.getRandomNumber_Exponential(mu_UE[service_type_gen]);
			service_time[1] = g.getRandomNumber_Exponential(mu_E[service_type_gen]);
			service_time[2] = g.getRandomNumber_Exponential(mu_EU[service_type_gen]);
			packet[i] = new Packet(i + 1, service_type_gen, arrival_time, service_time);
		}
		
		for(int i = 0; i < number_packet; i++) {
			for(int j = i + 1; j < number_packet; j++) {
				if(packet[i].arrival_time[0] > packet[j].arrival_time[0]) swap(packet, i, j);
			}
			packet[i].packet_index = i + 1;
		}
		
		/*
		for(int i = 0; i < number_packet; i++) {
			System.out.println("封包(" + (i+1) + "): " + packet[i].info());
		}
		*/
	}
	
	/**
	 * M/M/1 模型
	 * FIFO
	 */
	private static void m_m_1(int k) {
		boolean isNotLast = (k != queue_number - 1);
		double previous_packet_departure_time = 0;
		
		// 處理每一個封包
		for(int i = 0; i < number_packet; i++) {
			packet[i].waiting_queue_length[k] = 0;

			// 需等候
			if(packet[i].arrival_time[k] < previous_packet_departure_time) {
				packet[i].departure_time[k] = previous_packet_departure_time + packet[i].service_time[k];
				packet[i].waiting_time[k] = previous_packet_departure_time - packet[i].arrival_time[k];
				
				for(int j = i - 1; j >= 0; j--) {
					if(packet[i].arrival_time[k] < packet[j].departure_time[k]) {
						packet[i].waiting_queue_length[k]++;
						packet[i].remaining_time[k] = packet[i].deadline_time[k] - previous_packet_departure_time;
					}
					else break;
				}
				
				// 依到達時間進行排程
				packet[i].waiting_queue_length_scheduled[k] = packet[i].waiting_queue_length[k];
				for(int j = i - packet[i].waiting_queue_length[k] + 1; j < i; j++) {
					if(packet[i].arrival_time[k] < packet[j].arrival_time[k]) {
						packet[i].waiting_queue_length_scheduled[k] = i - j;
						swap(packet, i, j);
						
						packet[j].departure_time[k] = packet[j - 1].departure_time[k] + packet[j].service_time[k];
						packet[j].waiting_time[k] = packet[j - 1].departure_time[k] - packet[j].arrival_time[k];
						if(isNotLast) packet[j].arrival_time[k+1] = packet[j].departure_time[k];
						
						packet[i].departure_time[k] = packet[i - 1].departure_time[k] + packet[i].service_time[k];
						packet[i].waiting_time[k] = packet[i - 1].departure_time[k] - packet[i].arrival_time[k];
						if(isNotLast) packet[i].arrival_time[k+1] = packet[i].departure_time[k];
					}
				}
				
			}
			// 不需等候
			else {
				packet[i].departure_time[k] = packet[i].arrival_time[k] + packet[i].service_time[k];
				packet[i].waiting_time[k] = 0;	
			}
			// 更新前一個封包離開時間
			previous_packet_departure_time = packet[i].departure_time[k];
			if(isNotLast) packet[i].arrival_time[k+1] = packet[i].departure_time[k];
		}
	}
	
	/**
	 * M/M/1 queue
	 */
	private static void m_m_1_sjf(int k) {
		boolean isNotLast = (k != queue_number - 1);
		double previous_packet_departure_time = 0;
		
		// 處理每一個封包
		for(int i = 0; i < number_packet; i++) {
			packet[i].waiting_queue_length[k] = 0;
			
			// 需等候
			if(packet[i].arrival_time[k] < previous_packet_departure_time) {
				packet[i].departure_time[k] = previous_packet_departure_time + packet[i].service_time[k];
				packet[i].waiting_time[k] = previous_packet_departure_time - packet[i].arrival_time[k];
				
				for(int j = i - 1; j >= 0; j--) {
					if(packet[i].arrival_time[k] < packet[j].departure_time[k]) {
						packet[i].waiting_queue_length[k]++;
						packet[i].remaining_time[k] = packet[i].deadline_time[k] - previous_packet_departure_time;
					}
					else break;
				}
				
				// 依服務時間進行排程
				packet[i].waiting_queue_length_scheduled[k] = packet[i].waiting_queue_length[k];
				for(int j = i - packet[i].waiting_queue_length[k] + 1; j < i; j++) {
					if(packet[i].service_time[k] < packet[j].service_time[k]) {
						packet[i].waiting_queue_length_scheduled[k] = i - j;
						swap(packet, i, j);
						
						packet[j].departure_time[k] = packet[j - 1].departure_time[k] + packet[j].service_time[k];
						packet[j].waiting_time[k] = packet[j - 1].departure_time[k] - packet[j].arrival_time[k];
						if(isNotLast) packet[j].arrival_time[k+1] = packet[j].departure_time[k];
						
						packet[i].departure_time[k] = packet[i - 1].departure_time[k] + packet[i].service_time[k];
						packet[i].waiting_time[k] = packet[i - 1].departure_time[k] - packet[i].arrival_time[k];
						if(isNotLast) packet[i].arrival_time[k+1] = packet[i].departure_time[k];
					}
				}
			}
			// 不需等候
			else {
				packet[i].departure_time[k] = packet[i].arrival_time[k] + packet[i].service_time[k];
				packet[i].waiting_time[k] = 0;				
			}
			// 更新前一個封包離開時間
			previous_packet_departure_time = packet[i].departure_time[k];			
			if(isNotLast) packet[i].arrival_time[k+1] = packet[i].departure_time[k];
		}
	}
	
	/**
	 * M/M/1 queue
	 */
	private static void m_m_1_edf(int k) {
		boolean isNotLast = (k != queue_number - 1);
		double previous_packet_departure_time = 0;
		
		// 處理每一個封包
		for(int i = 0; i < number_packet; i++) {
			packet[i].waiting_queue_length[k] = 0;
			
			// 需等候
			if(packet[i].arrival_time[k] < previous_packet_departure_time) {
				packet[i].departure_time[k] = previous_packet_departure_time + packet[i].service_time[k];
				packet[i].waiting_time[k] = previous_packet_departure_time - packet[i].arrival_time[k];
				
				for(int j = i - 1; j >= 0; j--) {
					if(packet[i].arrival_time[k] < packet[j].departure_time[k]) {
						packet[i].waiting_queue_length[k]++;
						packet[i].remaining_time[k] = packet[i].deadline_time[k] - previous_packet_departure_time;
					}
					else break;
				}
				
				// 依剩餘時間進行排程
				packet[i].waiting_queue_length_scheduled[k] = packet[i].waiting_queue_length[k];
				for(int j = i - packet[i].waiting_queue_length[k] + 1; j < i; j++) {
					if(packet[i].remaining_time[k] < packet[j].remaining_time[k]) {
						packet[i].waiting_queue_length_scheduled[k] = i - j;
						swap(packet, i, j);
						
						packet[j].departure_time[k] = packet[j - 1].departure_time[k] + packet[j].service_time[k];
						packet[j].waiting_time[k] = packet[j - 1].departure_time[k] - packet[j].arrival_time[k];
						if(isNotLast) {
							packet[j].arrival_time[k+1] = packet[j].departure_time[k];
							packet[i].deadline_time[k+1] = packet[i].arrival_time[k+1] + constant_deadline_time;
						}
						
						packet[i].departure_time[k] = packet[i - 1].departure_time[k] + packet[i].service_time[k];
						packet[i].waiting_time[k] = packet[i - 1].departure_time[k] - packet[i].arrival_time[k];
						if(isNotLast) {
							packet[i].arrival_time[k+1] = packet[i].departure_time[k];
							packet[i].deadline_time[k+1] = packet[i].arrival_time[k+1] + constant_deadline_time;
						}
					}
				}
			}
			// 不需等候
			else {
				packet[i].departure_time[k] = packet[i].arrival_time[k] + packet[i].service_time[k];
				packet[i].waiting_time[k] = 0;				
			}
			// 更新前一個封包離開時間
			previous_packet_departure_time = packet[i].departure_time[k];			
			if(isNotLast) {
				packet[i].arrival_time[k+1] = packet[i].departure_time[k];
				packet[i].deadline_time[k+1] = packet[i].arrival_time[k+1] + constant_deadline_time;
			}
		}
	}
	
	/**
	 * 統計結果
	 */
	private static void summary() {
		double total_service_time[] = new double[queue_number];
		double total_waiting_time[] = new double[queue_number];
		double total_processing_time = 0;
		double simulation_time = packet[number_packet - 1].departure_time[queue_number - 1];
		
		// 顯示和統計每一個封包的時間
		for(int i = 0; i < number_packet; i++) {
			for(int k = 0; k < queue_number; k++) {
				total_service_time[k] += packet[i].service_time[k];
				total_waiting_time[k] += packet[i].waiting_time[k];
			}
			total_processing_time += packet[i].departure_time[queue_number - 1] - packet[i].arrival_time[0];
			
			System.out.println("封包(" + (i+1) + "): " + packet[i].info());
		}
		
		// 顯示統計結果
		for(int k = 0; k < queue_number; k++) {
			System.out.println("Queue_" + (k+1) + "服務時間: " + (total_service_time[k]/number_packet));
			System.out.println("Queue_" + (k+1) + "等候時間: " + (total_waiting_time[k]/number_packet));
			System.out.println("Queue_" + (k+1) + "等候長度: " + (total_waiting_time[k]/simulation_time));
			
			System.out.println("Queue_" + (k+1) + "__Average sojourn time: " + ((total_service_time[k] + total_waiting_time[k])/number_packet));
			
			
		}
		System.out.println("平均每個封包的處理時間: " + (total_processing_time/number_packet));
	}
	
	/**
	 * 封包處理順序交換
	 * @param packet 封包矩陣
	 * @param i 第i個封包
	 * @param j 第j個封包
	 */
	private static void swap(Packet packet[], int i, int j) {
		Packet temp = packet[i];
		packet[i] = packet[j];
		packet[j] = temp;
	}
}
