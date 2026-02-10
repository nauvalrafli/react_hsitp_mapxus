//
//  TranslationClass.swift
//  mapxus-hsitp-ios
//
//  Created by a mutant on 10/01/26.
//

import Foundation
import SwiftUI

/// zh-Hant = Traditional Chinese
/// zh-Hans = Simplified Chinese

class TranslationClass: ObservableObject {
    func general(code: String) -> String {
        switch code {
        case "zh-Hant":
            return ""
        case "zh-Hans":
            return ""
        default:
            return ""
        }
    }
    
    func indoorMaps(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "室內地圖"
        case "zh-Hans":
            return "室内地图"
        default:
            return "Indoor Maps"
        }
    }
    
    func indoorMapsHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "探索這棟建築"
        case "zh-Hans":
            return "探索这座建筑"
        default:
            return "Explore the Building"
        }
    }
    
    func indoorMapsSubHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "輕鬆取得每層樓的詳細室內地圖。"
        case "zh-Hans":
            return "轻松获取每层楼的详细室内地图。"
        default:
            return "Access detailed indoor maps for every floor with ease."
        }
    }
    
    func arNavigation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "擴增實境導航"
        case "zh-Hans":
            return "增强现实导航"
        default:
            return "AR Navigation"
        }
    }
    
    func arNavigationHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "視覺引導"
        case "zh-Hans":
            return "视觉引导"
        default:
            return "Visual Guidance"
        }
    }
    
    func arNavigationSubHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "在現實世界中，跟隨擴增實境導航箭頭到達目的地。"
        case "zh-Hans":
            return "在现实世界中，跟随增强现实导航箭头到达目的地。"
        default:
            return "Follow AR arrows in the real world to reach your destination."
        }
    }
    
    func smartSearch(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "智慧搜尋"
        case "zh-Hans":
            return "智能搜索"
        default:
            return "Smart Search"
        }
    }
    
    func smartSearchHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "尋找設施"
        case "zh-Hans":
            return "查找设施"
        default:
            return "Find Facilities"
        }
    }
    
    func smartSearchSubHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "立即尋找洗手間、公司、商店、餐廳和公用設施。"
        case "zh-Hans":
            return "立即查找洗手间、公司、商店、餐厅和公用设施。"
        default:
            return "Locate restrooms, company, shops, restaurant and utilities instantly."
        }
    }
    
    func permissionScreen(code: String) -> String {
        switch code {
        case "zh-Hant": return "導航設定"
        case "zh-Hans": return "导航设置"
        default:        return "Navigation Setup"
        }
    }

    func permissionScreenHeading(code: String) -> String {
        switch code {
        case "zh-Hant": return "位置與相機存取"
        case "zh-Hans": return "位置与相机存取"
        default:        return "Location & Camera Access"
        }
    }

    func permissionScreenSubHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "啟用這兩個權限，即可享受無縫的室內和擴增實境導航體驗。"
        case "zh-Hans":
            return "启用这两个权限，即可享受无缝的室内和增强现实导航体验。"
        default:
            return "Enable both permissions to enjoy a seamless Indoor and AR navigation experience."
        }
    }
    
    func openSettings(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "開啟設定"
        case "zh-Hans":
            return "打开设置"
        default:
            return "Open Settings"
        }
    }
    
    func getStarted(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "開始使用"
        case "zh-Hans":
            return "开始使用"
        default:
            return "Get Started"
        }
    }
    
    func exploreByMap(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "通过地图探索"
        case "zh-Hans":
            return "透過地圖探索"
        default:
            return "Explore by Map"
        }
    }
    
    func lokMaChauLoop(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "落馬洲環路"
        case "zh-Hans":
            return "落马洲环路"
        default:
            return "Lok Ma Chau Loop"
        }
    }
    
    func searchDestination(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "搜尋目的地"
        case "zh-Hans":
            return "搜索目的地"
        default:
            return "Search destination..."
        }
    }
    
    func direction(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "方向"
        case "zh-Hans":
            return "方向"
        default:
            return "Direction"
        }
    }
    
    func navigation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "導航"
        case "zh-Hans":
            return "导航"
        default:
            return "Navigation"
        }
    }
    
    func selectStartPoint(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "选择起点"
        case "zh-Hans":
            return "選擇起點"
        default:
            return "Select Start Point"
        }
    }
    
    func currentLocation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "当前位置"
        case "zh-Hans":
            return "目前位置"
        default:
            return "Current Location"
        }
    }
    
    func selectLocationFromMap(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "從地圖中選擇位置"
        case "zh-Hans":
            return "从地图中选择位置"
        default:
            return "Select Location from Map"
        }
    }
    
    func setStartLocation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "設定起始位置"
        case "zh-Hans":
            return "设置起始位置"
        default:
            return "Set Start Location"
        }
    }
    
    func routeType(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "路線類型"
        case "zh-Hans":
            return "路线类型"
        default:
            return "Route Type"
        }
    }
    
    func showRoute(code: String, state: Bool) -> String {
        switch code {
        case "zh-Hant":
            return state ? "隱藏路線" : "顯示路線"
        case "zh-Hans":
            return state ? "隐藏路线" : "显示路线"
        default:
            return state ? "Hide Route" : "Show Route"
        }
    }
    
    func startNavigation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "開始導航"
        case "zh-Hans":
            return "开始导航"
        default:
            return "Start Navigation"
        }
    }
    
    func latitude(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "緯度"
        case "zh-Hans":
            return "纬度"
        default:
            return "Lat"
        }
    }
    
    func longitude(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "經度"
        case "zh-Hans":
            return "经度"
        default:
            return "Lon"
        }
    }
    
    func dragMapTooltip(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "拖曳地圖，將圖釘放置在您想要的起始位置。"
        case "zh-Hans":
            return "拖动地图，将图钉放置在您想要的起始位置。"
        default:
            return "Drag the map to position the pin at your desired starting location."
        }
    }
    
    func arButtonTooltip(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "點擊即可開始您的AR導航體驗。"
        case "zh-Hans":
            return "点击即可开始您的AR导航体验。"
        default:
            return "Tap to begin your AR navigation experience."
        }
    }
    
    func destinationTooltip(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "您選擇的目的地。"
        case "zh-Hans":
            return "您选择的目的地。"
        default:
            return "Your selected destination."
        }
    }
    
    func startTooltip(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "您目前的起始位置。"
        case "zh-Hans":
            return "您当前的起始位置。"
        default:
            return "Your current starting location."
        }
    }
    
    func kudos(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "恭喜！"
        case "zh-Hans":
            return "恭喜！"
        default:
            return "Kudos!."
        }
    }
    
    func arrivedAtDestination(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "您已到達目的地"
        case "zh-Hans":
            return "您已到达目的地"
        default:
            return "You have arrived at the destination."
        }
    }
    
    func finished(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "完成的"
        case "zh-Hans":
            return "完成的"
        default:
            return "✓ Finished"
        }
    }
    
    func goPrevious(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "回上一頁"
        case "zh-Hans":
            return "返回上一页"
        default:
            return "↺ Go previous"
        }
    }
    
    func endNavigation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "結束導航？"
        case "zh-Hans":
            return "结束导航？"
        default:
            return "End the Navigation?"
        }
    }
    
    func youAreGoingToEndTheNavigation(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "您即將結束導航。"
        case "zh-Hans":
            return "您即将结束导航。"
        default:
            return "You are going to end the Navigation."
        }
    }
    
    func end(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "結束導航"
        case "zh-Hans":
            return "结束导航"
        default:
            return "End Navigation"
        }
    }
    
    func cancel(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "取消"
        case "zh-Hans":
            return "取消"
        default:
            return "Cancel"
        }
    }
    
    func second(plural: Bool, code: String) -> String {
        let space = (code == "en") ? " " : "" // Add space for English only
        switch code {
        case "zh-Hant":
            return "秒"
        case "zh-Hans":
            return "秒"
        default:
            return space + (plural ? "secs" : "sec")
        }
    }
    
    func minute(plural: Bool, code: String) -> String {
        let space = (code == "en") ? " " : "" // Add space for English only
        switch code {
        case "zh-Hant":
            return "分鐘"
        case "zh-Hans":
            return "分钟"
        default:
            return space + (plural ? "mins" : "min")
        }
    }
    
    func hour(plural: Bool, code: String) -> String {
        let space = (code == "en") ? " " : "" // Add space for English only
        switch code {
        case "zh-Hant": return "小時"
        case "zh-Hans": return "小时"
        default:        return space + (plural ? "hours" : "hour")
        }
    }
    
    func meter(plural: Bool, code: String) -> String {
        let space = (code == "en") ? " " : "" // Add space for English only
        switch code {
        case "zh-Hant": return "米"
        case "zh-Hans": return "米"
        default:        return space + "m"
        }
    }
    
    func faceYouriPhone(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "將攝影機對準："
        case "zh-Hans":
            return "将摄像头对准："
        default:
            return "Face your camera to:"
        }
    }
    
    func almostThere(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "快到了，繼續加油！"
        case "zh-Hans":
            return "快到了，继续加油！"
        default:
            return "Almost there. Keep going!."
        }
    }
    
    func compass(code: String) -> String {
        switch code {
        case "zh-Hant":
            return ""
        case "zh-Hans":
            return ""
        default:
            return ""
        }
    }
    
    func loading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "載入中"
        case "zh-Hans":
            return "加载中"
        default:
            return "Loading..."
        }
    }
    
    func offRoute(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "偏離路線"
        case "zh-Hans":
            return "偏离路线"
        default:
            return "Off Route"
        }
    }
    
    func allBuildings(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "所有建築"
        case "zh-Hans":
            return "所有建筑"
        default:
            return "All Buildings"
        }
    }
    
    func hsitp(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "洗手間已成功翻新"
        case "zh-Hans":
            return "洗手间已成功翻新"
        default:
            return "Hong Kong-Shenzhen Innovation And Technology Park"
        }
    }
    
    func washroomOccupancyRefreshedSuccessfully(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "所有建築"
        case "zh-Hans":
            return "所有建筑"
        default:
            return "Washroom has been refreshed successfully"
        }
    }
    
    func washroomOccupancyFailedToRefresh(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "刷新失敗，請重試！"
        case "zh-Hans":
            return "刷新失败，请重试！"
        default:
            return "Failed to refresh. Please try again!."
        }
    }
    
    func washroomOccupancyCouldntRefresh(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "無法查看洗手間使用！"
        case "zh-Hans":
            return "无法查看洗手间使用情况！"
        default:
            return "Couldn't access the Washroom occupancy!."
        }
    }
}
