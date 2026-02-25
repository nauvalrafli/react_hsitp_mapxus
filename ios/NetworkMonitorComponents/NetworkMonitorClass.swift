//
//  NetworkMonitorClass.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 08/02/26.
//

import Network
import SwiftUI
import Combine

class NetworkMonitorClass: ObservableObject {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "NetworkMonitor")
    
    @Published var isConnected: Bool = false
    @Published var isWifi: Bool = false

    init() {
        monitor.pathUpdateHandler = { path in
            DispatchQueue.main.async {
                self.isConnected = path.status == .satisfied
                // Check specifically for Wi-Fi
                self.isWifi = path.usesInterfaceType(.wifi)
            }
        }
        monitor.start(queue: queue)
    }
}
