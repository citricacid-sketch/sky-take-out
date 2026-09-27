"""
双跑对比脚本：调用 Python AI 服务，输出结果供人工/自动对比。

用法：
    python -m tests.evaluation.compare --provider python   # 调 Python
    python -m tests.evaluation.compare --provider java     # 调 Java (通过 Java 启动后的接口)

当前实现：仅输出 Python 端结果（Java 端需 Java 启动后手动对比）。
"""

import argparse
import json
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import URLError

from tests.evaluation.evaluation_set import (
    NL2SQL_EVALUATION_SET,
    CHAT_EVALUATION_SET,
    ORDER_PLAN_EVALUATION_SET,
)


BASE_URL = "http://127.0.0.1:8000"


def call_chat(message, user_id="eval_user"):
    return _post("/api/v1/chat", {"userId": user_id, "message": message})


def call_analysis(question):
    return _post("/api/v1/analysis/ask", {"question": question})


def call_order_plan(people, budget, tastes=None, excludes=None):
    return _post("/api/v1/order/plan", {
        "people": people,
        "budget": budget,
        "tastes": tastes or [],
        "excludes": excludes or [],
    })


def _post(path, body):
    url = BASE_URL + path
    data = json.dumps(body).encode("utf-8")
    req = Request(url, data=data, headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except URLError as e:
        return {"error": str(e)}
    except Exception as e:
        return {"error": str(e)}


def run_nl2sql_evaluation():
    print("=" * 60)
    print("NL2SQL 评测")
    print("=" * 60)
    passed = 0
    failed = 0
    for case in NL2SQL_EVALUATION_SET:
        print(f"\n[{case['id']}] {case['question']}")
        start = time.time()
        result = call_analysis(case["question"])
        elapsed = time.time() - start
        print(f"  耗时: {elapsed:.2f}s")
        if "error" in result:
            print(f"  错误: {result['error']}")
            failed += 1
            continue
        sql = result.get("sql", "")
        answer = result.get("answer", "")
        print(f"  SQL: {sql}")
        print(f"  回答: {answer[:100]}...")
        if case["should_reject"]:
            if "拒绝" in answer or "失败" in answer:
                print(f"  ✓ 正确拒绝")
                passed += 1
            else:
                print(f"  ✗ 应被拒绝但未拒绝")
                failed += 1
        else:
            if sql and "失败" not in answer:
                print(f"  ✓ 正常执行")
                passed += 1
            else:
                print(f"  ✗ 执行失败")
                failed += 1
    print(f"\nNL2SQL 结果: {passed} 通过, {failed} 失败")
    return passed, failed


def run_chat_evaluation():
    print("\n" + "=" * 60)
    print("客服对话评测")
    print("=" * 60)
    passed = 0
    for case in CHAT_EVALUATION_SET:
        print(f"\n[{case['id']}] {case['message']}")
        start = time.time()
        result = call_chat(case["message"])
        elapsed = time.time() - start
        print(f"  耗时: {elapsed:.2f}s")
        if "error" in result:
            print(f"  错误: {result['error']}")
            continue
        reply = result.get("reply", "")
        print(f"  回复: {reply[:100]}...")
        passed += 1
    print(f"\n客服对话结果: {passed}/{len(CHAT_EVALUATION_SET)} 通过")
    return passed, len(CHAT_EVALUATION_SET) - passed


def run_order_plan_evaluation():
    print("\n" + "=" * 60)
    print("点餐推荐评测")
    print("=" * 60)
    passed = 0
    failed = 0
    for case in ORDER_PLAN_EVALUATION_SET:
        print(f"\n[{case['id']}] {case['people']}人/¥{case['budget']}/口味={case['tastes']}")
        start = time.time()
        result = call_order_plan(case["people"], case["budget"], case["tastes"], case["excludes"])
        elapsed = time.time() - start
        print(f"  耗时: {elapsed:.2f}s")
        if "error" in result:
            print(f"  错误: {result['error']}")
            failed += 1
            continue
        items = result.get("items", [])
        total = result.get("total", 0)
        reason = result.get("reason", "")
        print(f"  推荐 {len(items)} 项, 总价 ¥{total}")
        print(f"  理由: {reason[:80]}...")
        if case["budget"] > 0 and len(items) == 0:
            print(f"  ✗ 预算>0 但无推荐")
            failed += 1
        else:
            print(f"  ✓ 正常")
            passed += 1
    print(f"\n点餐推荐结果: {passed} 通过, {failed} 失败")
    return passed, failed


def main():
    parser = argparse.ArgumentParser(description="AI 服务双跑对比")
    parser.add_argument("--provider", default="python", choices=["python", "java"])
    parser.add_argument("--base-url", default=BASE_URL)
    args = parser.parse_args()
    global BASE_URL
    BASE_URL = args.base_url

    print(f"评测目标: {args.provider} @ {BASE_URL}")

    nl2sql_pass, nl2sql_fail = run_nl2sql_evaluation()
    chat_pass, chat_fail = run_chat_evaluation()
    order_pass, order_fail = run_order_plan_evaluation()

    print("\n" + "=" * 60)
    print("总评")
    print("=" * 60)
    total_pass = nl2sql_pass + chat_pass + order_pass
    total_fail = nl2sql_fail + chat_fail + order_fail
    print(f"总计: {total_pass} 通过, {total_fail} 失败")
    sys.exit(0 if total_fail == 0 else 1)


if __name__ == "__main__":
    main()
