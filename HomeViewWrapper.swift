import UIKit
import SwiftUI

@objcMembers
public class HomeViewController: UIViewController {
  private var hostingController: UIHostingController<HomeView>?

  public override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
    super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
  }

  public required init?(coder: NSCoder) {
    super.init(coder: coder)
  }

  public override func loadView() {
    super.loadView()
    let home = HomeView()
    let hc = UIHostingController(rootView: home)
    addChild(hc)
    hc.view.translatesAutoresizingMaskIntoConstraints = false
    view.addSubview(hc.view)
    NSLayoutConstraint.activate([
      hc.view.topAnchor.constraint(equalTo: view.topAnchor),
      hc.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
      hc.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      hc.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)
    ])
    hc.didMove(toParent: self)
    hostingController = hc
  }

  public override func viewDidAppear(_ animated: Bool) {
    super.viewDidAppear(animated)
  }
}

@objcMembers
public class HomeViewWrapper: UIView {
  private var hostingController: UIHostingController<HomeView>?

  public override init(frame: CGRect) {
    super.init(frame: frame)
    embedHomeView()
  }

  public required init?(coder: NSCoder) {
    super.init(coder: coder)
    embedHomeView()
  }

  private func embedHomeView() {
    guard hostingController == nil else { return }
    let home = HomeView()
    let hc = UIHostingController(rootView: home)
    hostingController = hc
    // Find nearest view controller to attach the hosting controller
    if let parentVC = findViewController() {
      parentVC.addChild(hc)
      hc.view.translatesAutoresizingMaskIntoConstraints = false
      addSubview(hc.view)
      NSLayoutConstraint.activate([
        hc.view.topAnchor.constraint(equalTo: topAnchor),
        hc.view.bottomAnchor.constraint(equalTo: bottomAnchor),
        hc.view.leadingAnchor.constraint(equalTo: leadingAnchor),
        hc.view.trailingAnchor.constraint(equalTo: trailingAnchor)
      ])
      hc.didMove(toParent: parentVC)
    } else {
      // If no parent VC yet, add as a subview and rely on later layout
      hc.view.frame = bounds
      hc.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
      addSubview(hc.view)
    }
  }

  private func findViewController() -> UIViewController? {
    var responder: UIResponder? = self
    while responder != nil {
      responder = responder?.next
      if let vc = responder as? UIViewController { return vc }
    }
    return nil
  }
}
