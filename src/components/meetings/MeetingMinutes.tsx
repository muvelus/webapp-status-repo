import React, { useState, useEffect } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card'
import { Button } from '../ui/button'
import { Input } from '../ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { Badge } from '../ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../ui/tabs'
import { Textarea } from '../ui/textarea'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '../ui/dialog'
import { 
  Search, 
  Calendar, 
  Clock, 
  Users, 
  Video, 
  RefreshCw,
  Eye,
  Download,
  Filter
} from 'lucide-react'
import { MeetingMinutes as MeetingMinutesType, TimeFilter } from '../../types'
import { apiClient } from '../../utils/api'
import { toast } from 'sonner'

const MeetingMinutes: React.FC = () => {
  const { user } = useAuth()
  const [meetings, setMeetings] = useState<MeetingMinutesType[]>([])
  const [selectedMeeting, setSelectedMeeting] = useState<MeetingMinutesType | null>(null)
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedTimeFilter, setSelectedTimeFilter] = useState('week')
  const [regenerating, setRegenerating] = useState<number | null>(null)

  const timeFilters: TimeFilter[] = [
    {
      label: 'Today',
      value: 'today',
      startDate: new Date(new Date().setHours(0, 0, 0, 0)),
      endDate: new Date(new Date().setHours(23, 59, 59, 999))
    },
    {
      label: 'This Week',
      value: 'week',
      startDate: new Date(new Date().setDate(new Date().getDate() - 7)),
      endDate: new Date()
    },
    {
      label: 'Last 2 Weeks',
      value: '2weeks',
      startDate: new Date(new Date().setDate(new Date().getDate() - 14)),
      endDate: new Date()
    },
    {
      label: 'Last Month',
      value: 'month',
      startDate: new Date(new Date().setMonth(new Date().getMonth() - 1)),
      endDate: new Date()
    }
  ]

  useEffect(() => {
    fetchMeetings()
  }, [selectedTimeFilter])

  const fetchMeetings = async () => {
    try {
      setLoading(true)
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const response = await apiClient.get<{ meetings: MeetingMinutesType[] }>(
        `/api/meetings/my-meetings?startDate=${filter.startDate.toISOString().split('T')[0]}&endDate=${filter.endDate.toISOString().split('T')[0]}`
      )
      
      setMeetings(response.meetings || [])
    } catch (error) {
      console.error('Failed to fetch meetings:', error)
      toast.error('Failed to load meetings')
    } finally {
      setLoading(false)
    }
  }

  const searchMeetings = async () => {
    if (!searchQuery.trim()) {
      fetchMeetings()
      return
    }

    try {
      setLoading(true)
      const filter = timeFilters.find(f => f.value === selectedTimeFilter)
      if (!filter) return

      const response = await apiClient.get<{ meetings: MeetingMinutesType[] }>(
        `/api/meetings/search?query=${encodeURIComponent(searchQuery)}&startDate=${filter.startDate.toISOString().split('T')[0]}&endDate=${filter.endDate.toISOString().split('T')[0]}`
      )
      
      setMeetings(response.meetings || [])
    } catch (error) {
      console.error('Failed to search meetings:', error)
      toast.error('Failed to search meetings')
    } finally {
      setLoading(false)
    }
  }

  const regenerateMeetingMinutes = async (meetingId: number) => {
    try {
      setRegenerating(meetingId)
      const response = await apiClient.post<{ meeting: MeetingMinutesType }>(`/api/meetings/${meetingId}/regenerate`)
      
      setMeetings(prev => prev.map(m => m.id === meetingId ? response.meeting : m))
      if (selectedMeeting?.id === meetingId) {
        setSelectedMeeting(response.meeting)
      }
      
      toast.success('Meeting minutes regenerated successfully!')
    } catch (error) {
      console.error('Failed to regenerate meeting minutes:', error)
      toast.error('Failed to regenerate meeting minutes')
    } finally {
      setRegenerating(null)
    }
  }

  const downloadMeetingMinutes = (meeting: MeetingMinutesType) => {
    const content = `
Meeting Minutes
Title: ${meeting.meetingTitle}
Date: ${new Date(meeting.meetingDate).toLocaleDateString()}
Platform: ${meeting.meetingPlatform}
Duration: ${meeting.durationMinutes} minutes
Attendees: ${meeting.attendees}

AI Summary:
${meeting.aiSummary}

Key Points:
${meeting.keyPoints}

Action Items:
${meeting.actionItems}

Decisions Made:
${meeting.decisionsMade}

${meeting.transcript ? `\nFull Transcript:\n${meeting.transcript}` : ''}
    `.trim()

    const blob = new Blob([content], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `meeting-minutes-${meeting.meetingTitle.replace(/[^a-z0-9]/gi, '-').toLowerCase()}-${meeting.meetingDate.split('T')[0]}.txt`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  const getPlatformIcon = (platform: string) => {
    switch (platform) {
      case 'ZOOM':
        return <Video className="h-4 w-4" />
      case 'MICROSOFT_TEAMS':
        return <Users className="h-4 w-4" />
      default:
        return <Calendar className="h-4 w-4" />
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Meeting Minutes</h1>
          <p className="text-gray-600">View and search your meeting minutes</p>
        </div>
        <div className="flex space-x-3">
          <Select value={selectedTimeFilter} onValueChange={setSelectedTimeFilter}>
            <SelectTrigger className="w-48">
              <SelectValue placeholder="Select time period" />
            </SelectTrigger>
            <SelectContent>
              {timeFilters.map((filter) => (
                <SelectItem key={filter.value} value={filter.value}>
                  {filter.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Search Meetings</CardTitle>
          <CardDescription>
            Search through your meeting titles, summaries, and content
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex space-x-2">
            <Input
              placeholder="Search meetings..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && searchMeetings()}
              className="flex-1"
            />
            <Button onClick={searchMeetings}>
              <Search className="h-4 w-4 mr-2" />
              Search
            </Button>
            <Button onClick={fetchMeetings} variant="outline">
              <Filter className="h-4 w-4 mr-2" />
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Meeting List</CardTitle>
            <CardDescription>
              {meetings.length} meetings found
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3 max-h-96 overflow-y-auto">
              {meetings.map((meeting) => (
                <div
                  key={meeting.id}
                  className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                    selectedMeeting?.id === meeting.id ? 'bg-blue-50 border-blue-200' : 'hover:bg-gray-50'
                  }`}
                  onClick={() => setSelectedMeeting(meeting)}
                >
                  <div className="flex justify-between items-start mb-2">
                    <h4 className="font-medium text-sm">{meeting.meetingTitle}</h4>
                    <div className="flex items-center space-x-1">
                      {getPlatformIcon(meeting.meetingPlatform)}
                      <Badge variant="outline" className="text-xs">
                        {meeting.meetingPlatform}
                      </Badge>
                    </div>
                  </div>
                  <div className="flex items-center justify-between text-sm text-gray-600">
                    <div className="flex items-center space-x-2">
                      <Calendar className="h-3 w-3" />
                      <span>{new Date(meeting.meetingDate).toLocaleDateString()}</span>
                      <Clock className="h-3 w-3 ml-2" />
                      <span>{meeting.durationMinutes}m</span>
                    </div>
                    <Badge variant={meeting.processed ? 'default' : 'secondary'} className="text-xs">
                      {meeting.processed ? 'Processed' : 'Pending'}
                    </Badge>
                  </div>
                </div>
              ))}
              {meetings.length === 0 && (
                <p className="text-gray-500 text-center py-8">
                  No meetings found for this period
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <div>
                <CardTitle>
                  {selectedMeeting ? selectedMeeting.meetingTitle : 'Select a Meeting'}
                </CardTitle>
                <CardDescription>
                  {selectedMeeting && new Date(selectedMeeting.meetingDate).toLocaleDateString()}
                </CardDescription>
              </div>
              {selectedMeeting && (
                <div className="flex space-x-2">
                  <Button 
                    onClick={() => downloadMeetingMinutes(selectedMeeting)} 
                    variant="outline" 
                    size="sm"
                  >
                    <Download className="h-4 w-4" />
                  </Button>
                  {(user?.role === 'MANAGER' || user?.role === 'LEADER' || user?.role === 'ADMIN') && (
                    <Button 
                      onClick={() => regenerateMeetingMinutes(selectedMeeting.id)}
                      disabled={regenerating === selectedMeeting.id}
                      variant="outline" 
                      size="sm"
                    >
                      {regenerating === selectedMeeting.id ? (
                        <RefreshCw className="h-4 w-4 animate-spin" />
                      ) : (
                        <RefreshCw className="h-4 w-4" />
                      )}
                    </Button>
                  )}
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {selectedMeeting ? (
              <Tabs defaultValue="summary" className="space-y-4">
                <TabsList>
                  <TabsTrigger value="summary">Summary</TabsTrigger>
                  <TabsTrigger value="details">Details</TabsTrigger>
                  <TabsTrigger value="transcript">Transcript</TabsTrigger>
                </TabsList>

                <TabsContent value="summary" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                      <p className="text-sm font-medium text-gray-600">Duration</p>
                      <p className="text-lg">{selectedMeeting.durationMinutes} minutes</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-600">Attendees</p>
                      <p className="text-sm">{selectedMeeting.attendees}</p>
                    </div>
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">AI Summary</h4>
                    <Textarea
                      value={selectedMeeting.aiSummary}
                      readOnly
                      className="min-h-24 resize-none"
                    />
                  </div>
                </TabsContent>

                <TabsContent value="details" className="space-y-4">
                  <div>
                    <h4 className="font-medium mb-2">Key Points</h4>
                    <Textarea
                      value={selectedMeeting.keyPoints}
                      readOnly
                      className="min-h-20 resize-none"
                    />
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">Action Items</h4>
                    <Textarea
                      value={selectedMeeting.actionItems}
                      readOnly
                      className="min-h-20 resize-none"
                    />
                  </div>

                  <div>
                    <h4 className="font-medium mb-2">Decisions Made</h4>
                    <Textarea
                      value={selectedMeeting.decisionsMade}
                      readOnly
                      className="min-h-20 resize-none"
                    />
                  </div>
                </TabsContent>

                <TabsContent value="transcript" className="space-y-4">
                  {selectedMeeting.transcript ? (
                    <div>
                      <h4 className="font-medium mb-2">Full Transcript</h4>
                      <Textarea
                        value={selectedMeeting.transcript}
                        readOnly
                        className="min-h-64 resize-none"
                      />
                    </div>
                  ) : (
                    <div className="text-center py-8">
                      <p className="text-gray-500">No transcript available for this meeting</p>
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            ) : (
              <div className="text-center py-12">
                <Eye className="mx-auto h-12 w-12 text-gray-400" />
                <h3 className="mt-2 text-sm font-medium text-gray-900">No meeting selected</h3>
                <p className="mt-1 text-sm text-gray-500">
                  Choose a meeting from the list to view details
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default MeetingMinutes
