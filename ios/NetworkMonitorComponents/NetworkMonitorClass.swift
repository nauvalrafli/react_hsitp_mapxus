//
//  NetworkMonitorClass.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 08/02/26.
//

import Network
import SwiftUI

class NetworkMonitorClass: ObservableObject {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "Mapxus-Map-NetworkMonitor")
    @Published var isConnected: Bool = true

    init() {
        monitor.pathUpdateHandler = { path in
            DispatchQueue.main.async {
                self.isConnected = path.status == .satisfied
            }
        }
        monitor.start(queue: queue)
    }
}
