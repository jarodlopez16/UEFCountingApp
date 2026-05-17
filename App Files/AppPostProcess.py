import re

def postProcess(file, subBy=3):
    nums = []
    f = open(file)
    f.readline()
    line = f.readline()
    previousNum = float('inf')
    while line:
            cols = re.split("\t", line)
            text = cols[2]

            matches = re.findall(r'(?:negative\s+\d+|minus\s+\d+|-?\d+)', text, flags=re.IGNORECASE)

            for m in matches:
                if m.lower().startswith("minus"):
                    continue
                if m.lower().startswith("negative"): 
                    val = -int(m.split()[-1])
                else:
                    val = int(m)

                if val != previousNum:
                    nums.append(val)
                    previousNum = val

            line = f.readline()

    nums = mergeHundreds(nums)
    nums = removeSmallNumbers(nums)
    nums = removeRepeats(nums)
    nums = checkPrefix(nums)
    nums = checkOutliers(nums)
    return nums

def checkAccuracy(nums, subBy=3):
    commissions = 0
    omissions = 0
    variability = 0

    if len(nums) <= 1:
        return [0, 0, 0, 0]

    correct = 0
    prevDiff = None

    for i in range(1, len(nums)):
        prevNum = nums[i - 1]
        currNum = nums[i]
        currDiff = prevNum - currNum

        if currDiff == subBy:
            correct += 1

        if prevDiff is not None and currDiff != prevDiff:
            variability += 1

        if subBy > 0:
            if currDiff > subBy:
                omissions += (currDiff // subBy)

        else:
            if currDiff < subBy:
                omissions += (currDiff // subBy)

        if currDiff < subBy:
                commissions += 1

        prevDiff = currDiff

    return [correct, commissions, omissions, variability]

def getStats(file, subBy = 3):
    stats = []
    nums = postProcess(file)
    stats.append(len(nums))
    accuracy = checkAccuracy(nums, subBy)
    stats.append(accuracy[0])
    if len(nums) != 0:
        stats.append((len(nums) - 1) - accuracy[0])
    else:
        stats.append(0)
    stats.extend(accuracy[1:])
    return stats

def mergeHundreds(nums):
    merged = []
    i = 0

    while i < len(nums):

        if i + 1 < len(nums) and nums[i+1] == 100:
            merged.append(nums[i] * 100)
            i += 2
            continue

        if (i + 2 < len(nums)
            and 1 <= nums[i] <= 9
            and 1 <= nums[i+1] <= 9
            and 10 <= nums[i+2] < 100):
            
            merged.append(nums[i] * 100 + nums[i+1] * 10 + (nums[i+2] % 10))
            i += 3
            continue

        if (i + 1 < len(nums)
            and 1 <= nums[i] <= 9
            and 10 <= nums[i+1] < 100):
            
            merged.append(nums[i] * 100 + nums[i+1])
            i += 2
            continue

        merged.append(nums[i])
        i += 1

    return merged

def removeSmallNumbers(nums):
    cleaned = []
    i = 0

    while i < len(nums):

        if nums[i] < 10:
            start = i
            while i < len(nums) and nums[i] < 10:
                i += 1

            if i < len(nums) and 10 <= nums[i] < 100:
                cleaned.extend(nums[start:i])
            else:
                pass

            continue

        cleaned.append(nums[i])
        i += 1

    return cleaned

def removeRepeats(nums):
    cleaned = []
    previousNum = float('inf')

    for num in nums:
        if num != previousNum:
            cleaned.append(num)
        previousNum = num

    return cleaned

def checkPrefix(nums): 
    cleaned = [] 
    previousHundred = 0 
    doubleDigs = False 
    for num in nums: 
        if (len(str(num)) == 3) and (str(num)[0] * 100 == previousHundred): 
            cleaned.append(num) 
        elif (len(str(num)) == 3) and (str(num)[0] * 100 != previousHundred): 
            previousHundred = int(str(num)[0]) * 100 
            cleaned.append(num) 
        elif (len(str(num)) != 3) and not doubleDigs: 
            cleaned.append(previousHundred + num) 
        else: 
            cleaned.append(num) 
        
        if previousHundred == 100: 
            continue 
        
    return cleaned

def removeTail(nums, subBy):
    newNums = nums
    lastIdx = len(newNums) - 1
    if newNums[lastIdx] % 100 == 0:
        if newNums[lastIdx - 1] - newNums[lastIdx] != subBy:
            newNums.pop(lastIdx)
    elif newNums[lastIdx] % 10 == 0:
        if newNums[lastIdx - 1] - newNums[lastIdx] != subBy:
            newNums.pop(lastIdx)
    return newNums

def checkOutliers(nums, threshold=100):
    cleaned = []
    
    for i in range(len(nums)):
        if i == 0:
            if abs(nums[i+1] - nums[i]) <= threshold:
                cleaned.append(nums[i])
        elif i == len(nums) - 1:
            if abs(nums[i] - nums[i-1]) <= threshold:
                cleaned.append(nums[i])
        else:
            prev_diff = abs(nums[i] - nums[i-1])
            next_diff = abs(nums[i+1] - nums[i])
            
            if prev_diff <= threshold or next_diff <= threshold:
                cleaned.append(nums[i])
    
    return cleaned