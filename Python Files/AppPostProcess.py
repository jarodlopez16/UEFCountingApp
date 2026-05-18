import re

# Developed postprocessing functions for transcription data
# Based on patterns identified when listening to counting tasks and examining Whisper transcriptions

# Read transcription file, subBy is the number in which participants counted backwards by
# Default is 3, but can be changed (some participants were asked to count by 1s)
def postProcess(file, subBy=3):
    # Create a list to store counted numbers
    nums = []
    # Open transcription file and read lines
    f = open(file)
    f.readline()
    line = f.readline()
    # Initialize a previousNum variable to track last checked number
    previousNum = float('inf')
    # While line is not empty
    while line:
            # Split columns and retrieve transcription text
            cols = re.split("\t", line)
            text = cols[2]

            # Find sequences in transcription in which the word negative or minus were found with a number immediately following
            # Found that some participants would say the word minus when counting
            # i.e. "350 minus 3 is..", "347 minus 3 is.."
            matches = re.findall(r'(?:negative\s+\d+|minus\s+\d+|-?\d+)', text, flags=re.IGNORECASE)
            for m in matches:
                # If minus was found, disregard the following number
                if m.lower().startswith("minus"):
                    continue
                # If negative was found, convert the number portion into negative number
                if m.lower().startswith("negative"): 
                    val = -int(m.split()[-1])
                else:
                    val = int(m)
                # Only append the value if it is different from the number last appended
                if val != previousNum:
                    nums.append(val)
                    # Update the previous number tracker
                    previousNum = val
            # Read next line
            line = f.readline()

    if len(nums) <= 1:
        return nums
    # Call postprocessing functions
    nums = mergeHundreds(nums)
    nums = removeSmallNumbers(nums)
    nums = removeRepeats(nums)
    nums = checkPrefix(nums)
    # Remove tail only used for the purposes of the current paper being written
    # nums = removeTail(nums)
    # Return the list of cleaned numbers
    return nums

# Check the accuracy of counting
def checkAccuracy(nums, subBy=3):
    # Commissions is the number of extra counts
    commissions = 0
    # Omssions is the number of missed counts
    omissions = 0
    # Variability tracks how often the number counting by changes
    variability = 0
    ''' The calculation for these three outcomes are net yet final and were found to have lower ICC values than the
     total counted, correct subtractions, and incorrect subtractions outcomes. Will need further work '''

    # Return default outcomes for participants who counted 1 or less numbers
    if len(nums) <= 1:
        return [0, 0, 0, 0]

    # Track the correct counts
    correct = 0
    # Track the previous difference
    prevDiff = None

    # Interate through counted numbers
    for i in range(1, len(nums)):
        # Calculate difference
        prevNum = nums[i - 1]
        currNum = nums[i]
        currDiff = prevNum - currNum

        # If the difference matches the number they were supposed to subtract by, increase correct count by 1
        if currDiff == subBy:
            correct += 1
        # If the previous difference is different from the current difference, variability increased
        # Number being counted by changes
        if prevDiff is not None and currDiff != prevDiff:
            variability += 1
        # If the number being subtracted by is positive
        if subBy > 0:
            # If the difference is greater than the number meant to subtract by
            if currDiff > subBy:
                # Check how many correct subtractions were skipped
                omissions += (currDiff // subBy)
        # Some participants were asked to count upwards so this branch checks for that
        else:
            if currDiff < subBy:
                omissions += (currDiff // subBy)
        # If the current difference is less than the number meant to be subtracted by, it is an extra count
        if currDiff < subBy:
                commissions += 1

        # Reassign the previous difference tracker to use in calculation of variability with next pair of numbers
        prevDiff = currDiff
    # Return outcomes
    return [correct, commissions, omissions, variability]

# Function to get all the outcomes
def getStats(file, subBy = 3):
    # List to store outcomes
    stats = []
    # Post process the transcription file
    nums = postProcess(file)
    # Append the total amount of numbers counted
    stats.append(len(nums))
    # Get correct counts, commissions, omissions, and variability
    accuracy = checkAccuracy(nums, subBy)
    # Append the correct counts
    stats.append(accuracy[0])
    # Calculate and append the incorrect counts
    if len(nums) != 0:
        stats.append((len(nums) - 1) - accuracy[0])
    else:
        stats.append(0)
    # Append the last 3 outcomes
    stats.extend(accuracy[1:])
    # Return all 6 outcomes
    return stats

# During transcription by Whisper, some numbers were split into multiple parts
# i.e. Sometimes "254 was transcribed as 2 100 50 4" 
def mergeHundreds(nums):
    # Final list to store successfully merged or original numbers
    merged = []
    # Index to track our position in the list
    i = 0

    while i < len(nums):
        # Pattern: Single Digit, 100, Double Digit, Single Digit 
        # 2 100 40 5 -> 245
        if (i + 3 < len(nums) 
            and 1 <= nums[i] <= 9        # Single digit number
            and nums[i+1] == 100         # 100
            and 10 <= nums[i+2] < 100    # Double digit number
            and 1 <= nums[i+3] <= 9):    # Single digit number
            
            # Combine the numbers
            merged.append(nums[i] * 100 + nums[i+2] + nums[i+3])
            i += 4 # Move past all 4 elements
            continue

        # Pattern: Single Digit, 100, Double Digit
        # 2 100 40 -> 240
        if (i + 2 < len(nums)
            and 1 <= nums[i] <= 9        # Single digit number
            and nums[i+1] == 100         # 100
            and 10 <= nums[i+2] < 100):  # Double digit number
            
            # Combine the numbers
            merged.append(nums[i] * 100 + nums[i+2])
            i += 3 # Move past all 3 elements
            continue

        # Ignore numbers that don't meet patterns
        merged.append(nums[i])
        i += 1

    return merged

# Remove any small out of place numbers that could have been leftover after merging
# i.e. if someone counted backwards "333.. 2.. 1.. 330", transcription may have picked it up
def removeSmallNumbers(nums):
    # Create list of cleaned numbers
    cleaned = []
    i = 0
    # Iterate numbers
    while i < len(nums):
        # If the current number is less than 10
        if nums[i] < 10:
            # Keep track of the current index
            start = i
            # If the number is a single digit number and the number after
            while i < len(nums) and nums[i] < 10:
                # Increment i and check next number
                i += 1
            # If the following number is in the double digits
            if i < len(nums) and 10 <= nums[i] < 100:
                # It may not be an "out of place" number
                cleaned.extend(nums[start:i])
            # Skip the number because it is "out of place"
            else:
                pass
            # Skip the i increment because i was already incremented previously
            continue
        
        # If the number is not a single digit number, append it
        cleaned.append(nums[i])
        # Move to next index
        i += 1

    return cleaned

# Remove any consecutive repeating numbers from number list
# i.e. participant repeating last counted number while thinking of next one
def removeRepeats(nums):
    cleaned = []
    previousNum = float('inf')

    for num in nums:
        if num != previousNum:
            cleaned.append(num)
        previousNum = num

    return cleaned

# Another pattern was the dropping of the hundreds-place prefix prior to reaching the next hundred value 
# e.g., “325, 22, 19… 1, 298…”
def checkPrefix(nums): 
    # List to track cleaned numbers
    cleaned = [] 
    # Track the previous counted hundreds place
    previousHundred = 0 
    # Tracker to see if the person counting has reached the double digits
    doubleDigs = False 
    # Iterate through numbers
    for num in nums: 
        # If the previous counted hundred was 100, mark that they may move into the double digits naturally
        if previousHundred == 100: 
            doubleDigs = True
        # If it is a triple digit number and the hundreds place is equal to the last tracked hundred, append the number
        if (len(str(num)) == 3) and (str(num)[0] * 100 == previousHundred): 
            cleaned.append(num) 
        # If it is a triple digit number and the hundreds place is different from the last tracked hundred, change the hundreds tracker and append the numbers
        elif (len(str(num)) == 3) and (str(num)[0] * 100 != previousHundred): 
            previousHundred = int(str(num)[0]) * 100 
            cleaned.append(num) 
        # If the participant cannot naturally be in the double digits and the number counted is not a triple digit number, adjust the number
        elif (len(str(num)) != 3) and not doubleDigs: 
            cleaned.append(previousHundred + num) 
        # Any other case, append the number
        else: 
            cleaned.append(num) 
        
    return cleaned

# Postprocessing function only for paper because only first 60 seconds of dual task are used and some numbers may be cut off by clipping
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