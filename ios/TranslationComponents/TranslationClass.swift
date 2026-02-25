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
    
    func settings(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "設定"
        case "zh-Hans":
            return "设置"
        default:
            return "Settings"
        }
    }
    
    func generalSettings(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "一般的"
        case "zh-Hans":
            return "一般的"
        default:
            return "General"
        }
    }
    
    func language(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "語言"
        case "zh-Hans":
            return "语言"
        default:
            return "Language"
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
    
    func internetIntroduction(code: String) -> String {
        switch code {
        case "zh-Hant": return "網路連線"
        case "zh-Hans": return "网络连接"
        default:        return "Internet Connection"
        }
    }

    func internetIntroductionHeading(code: String) -> String {
        switch code {
        case "zh-Hant": return "Wi-Fi 或行動數據"
        case "zh-Hans": return "Wi-Fi 或移动数据"
        default:        return "Wi-Fi or Cellular Data"
        }
    }

    func internetIntroductionSubHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "請連接互聯網，享受流暢的室內和擴增實境導航體驗。"
        case "zh-Hans":
            return "请连接互联网，享受流畅的室内和增强现实导航体验。"
        default:
            return "Please connect to the internet to enjoy a seamless Indoor and AR navigation experience."
        }
    }
    
    func wifiIntroduction(code: String) -> String {
        switch code {
        case "zh-Hant": return "開啟 Wi-Fi"
        case "zh-Hans": return "开启 Wi-Fi"
        default:        return "Enable Wi-Fi"
        }
    }

    func wifiIntroductionHeading(code: String) -> String {
        return "Wi-Fi" // Consistent across all languages
    }

    func wifiIntroductionSubHeading(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "請啟用室內導航所需的Wi-Fi功能。"
        case "zh-Hans":
            return "请启用室内导航所需的Wi-Fi功能。"
        default:
            return "Please enable Wi-Fi for Indoor navigation."
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
    
    func gpsTooltip(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "點擊以顯示您的位置並自動旋轉地圖。"
        case "zh-Hans":
            return "点击以显示您的位置并自动旋转地图。"
        default:
            return "Tap to show your location and auto-rotate the map."
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
    
    func offRouteAlertMessage(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "您似乎偏離了導航路徑。是否要重新計算？"
        case "zh-Hans":
            return "您似乎偏离了导航路径。是否要重新计算？"
        default:
            return "You seem to have moved away from the navigation path. Would you like to recalculate?"
        }
    }
    
    func offRouteRecalculateButton(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "重新計算"
        case "zh-Hans":
            return "重新计算"
        default:
            return "Recalculate"
        }
    }
    
    func dismiss(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "解僱"
        case "zh-Hans":
            return "解雇"
        default:
            return "Dismiss"
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
            return "洗手間使用情況已成功更新。"
        case "zh-Hans":
            return "洗手间使用情况已成功更新。"
        default:
            return "Washroom Occupancy has been refreshed successfully."
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
            return "Couldn't access the Washroom Occupancy!."
        }
    }
    
    func mapRotationIsActive(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "地圖旋轉已啟用"
        case "zh-Hans":
            return "地图旋转已启用"
        default:
            return "Map rotation is active"
        }
    }
    
    func mapRotationIsNotActive(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "地圖旋轉功能未啟用"
        case "zh-Hans":
            return "地图旋转功能未启用"
        default:
            return "Map rotation isn't active"
        }
    }
    
    func noInternetConnection(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "無網路連線"
        case "zh-Hans":
            return "无网络连接"
        default:
            return "No Internet Connection"
        }
    }
    
    func failedToRefreshInternetConnection(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "網路連線已成功刷新"
        case "zh-Hans":
            return "网络连接已成功刷新"
        default:
            return "Internet connection refreshed successfully"
        }
    }
    
    func failedToRefreshInternetConnectionAfterMultipleAttemps(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "多次嘗試連線後仍然失敗。"
        case "zh-Hans":
            return "多次尝试连接后仍然失败。"
        default:
            return "Connection failed after multiple attempts."
        }
    }
    
    func categoryNotFound(category: String, code: String) -> String {
        switch code {
        case "zh-Hant":
            // Format: 此建築物內沒有 [類別]
            return "此建築物內沒有\(category)"
        case "zh-Hans":
            // Format: 此建筑物内没有 [类别]
            return "此建筑物内没有\(category)"
        default:
            // Format: No [Category] found in this building
            return "No \(category) found in this building"
        }
    }
    
    func enableWiFi(lang: String) -> String {
        switch lang {
        case "zh-Hant": return "開啟 Wi-Fi"
        case "zh-Hans": return "开启 Wi-Fi"
        default:        return "Enable Wi-Fi"
        }
    }
    
    func textToSpeech(code: String, isEnabled: Bool) -> String {
        switch code {
        case "zh-Hant":
            return isEnabled ? "語音朗讀已開啟" : "語音朗讀已關閉"
        case "zh-Hans":
            return isEnabled ? "语音朗读已开启" : "语音朗读已关闭"
        default:
            return isEnabled ? "Text to speech is enabled" : "Text to speech is disabled"
        }
    }
    
    func floorChanged(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "樓層已更換"
        case "zh-Hans":
            return "楼层已更换"
        default:
            return "Floor Changed"
        }
    }
    
    func floorChangedAlertMessage(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "你已經到達下一層了嗎？"
        case "zh-Hans":
            return "你已经到达下一层了吗？"
        default:
            return "Have you arrived at the next floor?"
        }
    }
    
    func buildingChanged(code: String) -> String {
        switch code {
        case "zh-Hant":
            return "建築物已更改"
        case "zh-Hans":
            return "建筑已更改"
        default:
            return "Building Changed"
        }
    }
    
    func buildingChangedAlertMessageLeave(code: String, buildingNumber: String) -> String {
        let formattedName = formatBuildingName(buildingNumber, code: code)
        
        switch code {
        case "zh-Hant":
            return !buildingNumber.isEmpty ? "您是否已離開\(formattedName)？" : "您是否已離開建築物？"
        case "zh-Hans":
            return !buildingNumber.isEmpty ? "您是否已离开\(formattedName)？" : "您是否已离开建筑物？"
        default:
            return !buildingNumber.isEmpty ? "Have you left \(formattedName)?" : "Have you left the building?"
        }
    }
    
    func buildingChangedAlertMessageEnter(code: String, buildingNumber: String) -> String {
        let formattedName = formatBuildingName(buildingNumber, code: code)
        
        switch code {
        case "zh-Hant":
            return !buildingNumber.isEmpty ? "您是否已進入\(formattedName)？" : "您是否已進入下一棟建築？"
        case "zh-Hans":
            return !buildingNumber.isEmpty ? "您是否已进入\(formattedName)？" : "您是否已进入下一栋建筑？"
        default:
            return !buildingNumber.isEmpty ? "Have you entered \(formattedName)?" : "Have you entered the next building?"
        }
    }
    
    private func formatBuildingName(_ name: String, code: String) -> String {
        let numberOnly = name.components(separatedBy: CharacterSet.decimalDigits.inverted).joined()
        
        switch code {
        case "zh-Hant", "zh-Hans":
            return !numberOnly.isEmpty ? "\(numberOnly)座" : name
        default:
            return name
        }
    }
}
