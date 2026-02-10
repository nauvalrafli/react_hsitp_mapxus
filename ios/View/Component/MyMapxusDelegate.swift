//
//  MyMapxusDelegate.swift
//  mapxus-hsitp-ios
//
//  Created by dev01 on 14/05/25.
//
import MapxusMapSDK

class MapxusDelegate : MapxusMapDelegate {
    func isEqual(_ object: Any?) -> Bool {
        return false
        
    }
    
    func map(_ map: MapxusMap, didSingleTapAt coordinate: CLLocationCoordinate2D) {
        print("Single tapped at: \(coordinate.latitude), \(coordinate.longitude)")
    }
    
    var hash: Int = 0
    
    var superclass: AnyClass?
    
    func `self`() -> Self {
        self
    }
    
    func perform(_ aSelector: Selector!) -> Unmanaged<AnyObject>! {
        return nil
    }
    
    func perform(_ aSelector: Selector!, with object: Any!) -> Unmanaged<AnyObject>! {
        return nil
    }
    
    func perform(_ aSelector: Selector!, with object1: Any!, with object2: Any!) -> Unmanaged<AnyObject>! {
        return nil
    }
    
    func isProxy() -> Bool {
        return false
    }
    
    func isKind(of aClass: AnyClass) -> Bool {
        return false
    }
    
    func isMember(of aClass: AnyClass) -> Bool {
        return false
    }
    
    func conforms(to aProtocol: Protocol) -> Bool {
        return false
    }
    
    func responds(to aSelector: Selector!) -> Bool {
        return false
    }
    
    var description: String = ""
    
    
}
