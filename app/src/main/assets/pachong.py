import requests
from bs4 import BeautifulSoup
import json
import time


def get_page_content(url):
    """请求并获取指定URL的页面内容"""
    try:
        response = requests.get(url, headers={'User-Agent': 'Mozilla/5.0'})
        response.raise_for_status()
        return response.text
    except requests.exceptions.RequestException as e:
        print(f"请求页面失败: {url}, 错误: {e}")
        return None


def get_bird_details(detail_url):
    """从鸟类详情页抓取所有文本信息"""
    if not detail_url:
        return "详情链接不存在"

    html = get_page_content(detail_url)
    if not html:
        return "无法获取详情页面内容"

    soup = BeautifulSoup(html, 'html.parser')
    # 查找主要内容区域
    content_div = soup.find('div', id='mw-content-text')
    if not content_div:
        return "未找到主要内容区域"

    # 提取所有段落文本
    paragraphs = content_div.find_all('p')
    details = "\n".join(p.get_text() for p in paragraphs)
    return details.strip()


def scrape_bird_list():
    """主函数，用于抓取中国鸟类列表并保存为JSON"""
    main_url = "https://zh.wikipedia.org/wiki/%E4%B8%AD%E5%9B%BD%E9%B8%9F%E7%B1%BB%E5%88%97%E8%A1%A8"
    base_wiki_url = "https://zh.wikipedia.org"

    print(f"开始抓取主列表页面: {main_url}")
    main_html = get_page_content(main_url)
    if not main_html:
        print("无法获取主列表页面，程序终止。")
        return

    soup = BeautifulSoup(main_html, 'html.parser')
    table = soup.find('table', class_='wikitable')
    if not table:
        print("未在页面中找到鸟类数据表格。")
        return

    all_birds_data = []
    current_order = ""
    current_family = ""

    rows = table.find_all('tr')
    print(f"找到了 {len(rows)} 行表格数据，开始逐行解析...")

    for i, row in enumerate(rows):
        cols = row.find_all(['td', 'th'])
        if not cols:
            continue

        # 检查是否是“目”的标题行
        if cols[0].get('colspan') == '6':
            current_order = cols[0].get_text(strip=True)
            current_family = ""
            print(f"\n找到新的目: {current_order}")
            continue

        # 检查是否是“科”的标题行 (通常编号列为空，但中文名列有内容)
        # 鸭科的例子：<td>1</td><td>Anatidae</td><td><a href="/wiki/%E9%B8%AD%E7%A7%91" title="鸭科">鸭科</a></td>...
        # 实际科的行的第一个td是编号，所以我们检查第三个单元格是否有链接且第一个是数字
        # 有些科没有编号，所以我们检查第一个单元格是否是空的或者不是数字
        first_col_text = cols[0].get_text(strip=True)
        if len(cols) > 2 and not first_col_text.isdigit() and cols[2].find('a'):
            # 排除表头行
            if '中文名' in cols[2].get_text(strip=True):
                continue
            current_family = cols[2].get_text(strip=True)
            print(f"  找到新的科: {current_family}")
            continue

        # 检查是否是普通的鸟类数据行
        if first_col_text.isdigit():
            try:
                number = cols[0].get_text(strip=True)
                scientific_name = cols[1].get_text(strip=True)

                chinese_name_tag = cols[2].find('a')
                chinese_name = chinese_name_tag.get_text(strip=True) if chinese_name_tag else cols[2].get_text(
                    strip=True)
                detail_link_suffix = chinese_name_tag['href'] if chinese_name_tag and chinese_name_tag.has_attr(
                    'href') else ''
                detail_full_link = base_wiki_url + detail_link_suffix if detail_link_suffix else ''

                iucn_status = cols[3].get_text(strip=True)
                protection_level = cols[4].get_text(strip=True)

                print(f"  正在处理: {number} - {chinese_name}...")

                # 抓取鸟类详情
                bird_details_text = get_bird_details(detail_full_link)
                # 礼貌延迟，避免被封
                time.sleep(0.5)

                bird_data = {
                    "编号": number,
                    "学名": scientific_name,
                    "中文名": chinese_name,
                    "所属目": current_order,
                    "所属科": current_family,
                    "详情链接": detail_full_link,
                    "IUCN红色名录": iucn_status if iucn_status else "无数据",
                    "国家保护等级": protection_level if protection_level else "无数据",
                    "鸟类详情": bird_details_text
                }

                all_birds_data.append(bird_data)

            except (IndexError, AttributeError) as e:
                print(f"    解析行失败: {row.get_text(strip=True)}, 错误: {e}")

    # 将所有数据写入JSON文件
    output_filename = 'bird_data.json'
    print(f"\n所有数据抓取和解析完成，正在写入到文件: {output_filename}")
    with open(output_filename, 'w', encoding='utf-8') as f:
        json.dump(all_birds_data, f, ensure_ascii=False, indent=4)

    print(f"成功！数据已保存到 {output_filename}")


if __name__ == '__main__':
    scrape_bird_list()